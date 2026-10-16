/**
 * ScrapChef Puter Serverless Worker (Production-Grade Architecture)
 * 
 * Implements the full Puter.js User-Pays Model & Stateless Worker Architecture
 * following the Learn with Sumit (LWS) Puter.js Master Specification:
 * 
 * 1. Global Metrics & Developer-Pays (`me.puter`):
 *    - Uses `me.puter.kv` to track global cross-user statistics (e.g., total recipes generated).
 *    - Atomic increments via `me.puter.kv.incr()`.
 *    - Route: GET /stats and GET /health.
 * 
 * 2. User-Pays AI Inference & Streaming (`user.puter.ai`):
 *    - Resolves caller context via `puter-auth: <token>` header.
 *    - Uses `user.puter.ai.chat()` so billing is strictly attributed to caller.
 *    - Defensive streaming SSE with explicit `chunk.type === 'error'` inspection.
 *    - Supports multi-model catalog (gpt-5-nano, claude-3-5-sonnet, gemini-2.5-flash).
 * 
 * 3. User Cloud File Storage (`user.puter.fs`):
 *    - Persists recipes directly to the caller's sandboxed cloud drive: ~/AppData/<app_id>/recipes/<id>.json.
 *    - Enforces `{ createMissingParents: true }`.
 *    - Directory traversal via `readdir`, parallel `Promise.all`, and `fs.delete`.
 * 
 * 4. User Key-Value Preferences (`user.puter.kv`):
 *    - Saves user settings (e.g., `preferred_model`, dietary restrictions) to caller's private KV.
 *    - Validates key (< 1 KB) and value (< 400 KB) limits to avoid KEY_TOO_LARGE / VALUE_TOO_LARGE.
 * 
 * 5. Robustness & State Safety:
 *    - Strict `try...catch...finally` patterns so async streams and endpoints never hang.
 */

const GLOBAL_COUNTER_KEY = 'scrapchef-total-recipes-generated';
const PREFERRED_MODEL_KEY = 'preferred_model';

const SUPPORTED_MODELS = [
    { id: 'gpt-5-nano', name: 'GPT-5 Nano (Fast & Cheap)', default: true },
    { id: 'claude-3-5-sonnet', name: 'Claude 3.5 Sonnet' },
    { id: 'gemini-2.5-flash', name: 'Gemini 2.5 Flash' }
];

// ---------------------------------------------------------------------------
// 1. GLOBAL & HEALTH ENDPOINTS (Developer Context: me.puter)
// ---------------------------------------------------------------------------

// Route: Health check
router.get('/health', async () => {
    return new Response(JSON.stringify({
        status: 'ok',
        service: 'scrapchef-puter-worker',
        timestamp: new Date().toISOString()
    }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
    });
});

// Route: Global Analytics / Stats (Uses me.puter.kv for developer-owned global state)
router.get('/stats', async () => {
    try {
        let count = 0;
        if (typeof me !== 'undefined' && me.puter && me.puter.kv) {
            count = (await me.puter.kv.get(GLOBAL_COUNTER_KEY)) || 0;
        }
        return new Response(JSON.stringify({
            status: 'ok',
            totalRecipesGenerated: count,
            supportedModels: SUPPORTED_MODELS,
            timestamp: new Date().toISOString()
        }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' }
        });
    } catch (err) {
        return new Response(JSON.stringify({
            status: 'degraded',
            totalRecipesGenerated: 0,
            error: err.message
        }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' }
        });
    }
});

// ---------------------------------------------------------------------------
// 2. RECIPE INFERENCE & STREAMING (User Context: user.puter.ai)
// ---------------------------------------------------------------------------

// Helper: Validate caller context
function assertUserAuth(user) {
    if (!user || !user.puter) {
        return new Response(JSON.stringify({
            error: 'Unauthorized: Missing or invalid puter-auth token header.',
            code: 'AUTH_REQUIRED'
        }), {
            status: 401,
            headers: { 'Content-Type': 'application/json' }
        });
    }
    return null;
}

// Route: Recipe generation (Unary JSON)
router.post('/api/recipe/generate', async ({ request, user }) => {
    const unauthResponse = assertUserAuth(user);
    if (unauthResponse) return unauthResponse;

    try {
        const body = await request.json();
        const {
            ingredients = [],
            tools = [],
            maxTimeMinutes = 0,
            dietaryPreferences = [],
            cuisineCountry = '',
            model = 'gpt-5-nano'
        } = body;

        if (!ingredients.length || ingredients.length < 1) {
            return new Response(JSON.stringify({
                error: 'Bad Request: At least one ingredient is required.'
            }), {
                status: 400,
                headers: { 'Content-Type': 'application/json' }
            });
        }

        // Build prompt (preserves ScrapChef prompt contract §10.6)
        const systemPrompt = `You are ScrapChef, a zero-waste culinary assistant. Generate a single realistic recipe using ONLY the ingredients provided. Output MUST be valid JSON adhering strictly to the ScrapChef recipe schema.`;
        const userPrompt = `Ingredients: ${ingredients.join(', ')}\nTools: ${tools.join(', ')}\nMax Time: ${maxTimeMinutes > 0 ? maxTimeMinutes + ' minutes' : 'No limit'}\nDietary: ${dietaryPreferences.join(', ')}\nCuisine: ${cuisineCountry}`;

        // Call Puter AI under calling user's quota/billing
        const response = await user.puter.ai.chat([
            { role: 'system', content: systemPrompt },
            { role: 'user', content: userPrompt }
        ], {
            model: model,
            stream: false
        });

        // Increment global counter asynchronously under developer context
        if (typeof me !== 'undefined' && me.puter && me.puter.kv) {
            me.puter.kv.incr(GLOBAL_COUNTER_KEY).catch(() => {});
        }

        const replyContent = response.message?.content || response;

        return new Response(JSON.stringify({
            success: true,
            model: model,
            recipe: replyContent
        }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' }
        });

    } catch (err) {
        return new Response(JSON.stringify({
            error: 'Inference error',
            message: err.message || String(err)
        }), {
            status: 500,
            headers: { 'Content-Type': 'application/json' }
        });
    }
});

// Route: Recipe generation (Streaming SSE with explicit error chunk handling)
router.post('/api/recipe/stream', async ({ request, user }) => {
    if (!user || !user.puter) {
        return new Response('event: error\ndata: {"error":"Unauthorized: Missing or invalid puter-auth token header."}\n\n', {
            status: 401,
            headers: { 'Content-Type': 'text/event-stream' }
        });
    }

    try {
        const body = await request.json();
        const {
            ingredients = [],
            tools = [],
            maxTimeMinutes = 0,
            dietaryPreferences = [],
            cuisineCountry = '',
            model = 'gpt-5-nano'
        } = body;

        const systemPrompt = `You are ScrapChef, a zero-waste culinary assistant. Stream the recipe using strict JSON delimiters.`;
        const userPrompt = `Ingredients: ${ingredients.join(', ')}\nTools: ${tools.join(', ')}\nMax Time: ${maxTimeMinutes > 0 ? maxTimeMinutes + ' minutes' : 'No limit'}\nDietary: ${dietaryPreferences.join(', ')}\nCuisine: ${cuisineCountry}`;

        const stream = await user.puter.ai.chat([
            { role: 'system', content: systemPrompt },
            { role: 'user', content: userPrompt }
        ], {
            model: model,
            stream: true
        });

        // Increment global counter on start of stream
        if (typeof me !== 'undefined' && me.puter && me.puter.kv) {
            me.puter.kv.incr(GLOBAL_COUNTER_KEY).catch(() => {});
        }

        // Pipe streaming response back as Server-Sent Events (SSE)
        const encoder = new TextEncoder();
        const readable = new ReadableStream({
            async start(controller) {
                try {
                    for await (const chunk of stream) {
                        // Bug 5 fix from tutorial: explicit error chunk detection
                        if (chunk && chunk.type === 'error') {
                            throw new Error(chunk.error?.message || 'Upstream AI stream encountered an error chunk');
                        }

                        const text = chunk.text || chunk.message?.content || '';
                        if (text) {
                            controller.enqueue(encoder.encode(`data: ${JSON.stringify({ text })}\n\n`));
                        }
                    }
                    controller.enqueue(encoder.encode('data: [DONE]\n\n'));
                    controller.close();
                } catch (e) {
                    controller.enqueue(encoder.encode(`event: error\ndata: {"error":"${e.message}"}\n\n`));
                    controller.close();
                }
            }
        });

        return new Response(readable, {
            status: 200,
            headers: {
                'Content-Type': 'text/event-stream',
                'Cache-Control': 'no-cache',
                'Connection': 'keep-alive'
            }
        });

    } catch (err) {
        return new Response(`event: error\ndata: {"error":"${err.message}"}\n\n`, {
            status: 500,
            headers: { 'Content-Type': 'text/event-stream' }
        });
    }
});

// ---------------------------------------------------------------------------
// 3. USER CLOUD STORAGE ENDPOINTS (User Context: user.puter.fs)
// ---------------------------------------------------------------------------

// Route: Save recipe to user's private sandboxed cloud drive (~/AppData/<app_id>/recipes/<id>.json)
router.post('/api/recipes/save', async ({ request, user }) => {
    const unauth = assertUserAuth(user);
    if (unauth) return unauth;

    try {
        const { id, title, recipe } = await request.json();
        if (!id || !recipe) {
            return new Response(JSON.stringify({ error: 'Recipe id and recipe payload are required.' }), {
                status: 400,
                headers: { 'Content-Type': 'application/json' }
            });
        }

        const filePath = `recipes/${id}.json`;
        const payload = JSON.stringify({
            id,
            title: title || recipe.name || 'Untitled Recipe',
            recipe,
            updatedAt: Date.now()
        });

        // Enforce createMissingParents as documented in tutorial section 5.3
        await user.puter.fs.write(filePath, payload, { createMissingParents: true });

        return new Response(JSON.stringify({ success: true, path: filePath }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' }
        });
    } catch (err) {
        return new Response(JSON.stringify({ error: err.message || 'Failed to save recipe to cloud' }), {
            status: 500,
            headers: { 'Content-Type': 'application/json' }
        });
    }
});

// Route: List all user recipes from cloud drive
router.get('/api/recipes', async ({ user }) => {
    const unauth = assertUserAuth(user);
    if (unauth) return unauth;

    try {
        let files = [];
        try {
            files = await user.puter.fs.readdir('recipes');
        } catch (e) {
            // Directory does not exist yet; return empty list gracefully
            return new Response(JSON.stringify({ recipes: [] }), {
                status: 200,
                headers: { 'Content-Type': 'application/json' }
            });
        }

        // Parallel read using Promise.all as documented in tutorial section 5.3
        const recipePromises = files.map(async (file) => {
            try {
                const blob = await user.puter.fs.read(file.path);
                const text = await blob.text();
                return JSON.parse(text);
            } catch (readErr) {
                return null;
            }
        });

        const loaded = (await Promise.all(recipePromises)).filter(Boolean);
        loaded.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0));

        return new Response(JSON.stringify({ recipes: loaded }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' }
        });
    } catch (err) {
        return new Response(JSON.stringify({ error: err.message || 'Failed to load recipes' }), {
            status: 500,
            headers: { 'Content-Type': 'application/json' }
        });
    }
});

// Route: Delete a recipe from user's cloud drive
router.delete('/api/recipes/:id', async ({ params, user }) => {
    const unauth = assertUserAuth(user);
    if (unauth) return unauth;

    try {
        const recipeId = params.id;
        const filePath = `recipes/${recipeId}.json`;
        await user.puter.fs.delete(filePath);

        return new Response(JSON.stringify({ success: true, deleted: recipeId }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' }
        });
    } catch (err) {
        return new Response(JSON.stringify({ error: err.message || 'Failed to delete recipe' }), {
            status: 500,
            headers: { 'Content-Type': 'application/json' }
        });
    }
});

// ---------------------------------------------------------------------------
// 4. USER KEY-VALUE PREFERENCES (User Context: user.puter.kv)
// ---------------------------------------------------------------------------

// Route: Get user preferred AI model
router.get('/api/preferences/model', async ({ user }) => {
    const unauth = assertUserAuth(user);
    if (unauth) return unauth;

    try {
        const preferredModel = (await user.puter.kv.get(PREFERRED_MODEL_KEY)) || 'gpt-5-nano';
        return new Response(JSON.stringify({ preferredModel }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' }
        });
    } catch (err) {
        return new Response(JSON.stringify({ preferredModel: 'gpt-5-nano', error: err.message }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' }
        });
    }
});

// Route: Set user preferred AI model (with KV limit validation: key < 1KB, value < 400KB)
router.post('/api/preferences/model', async ({ request, user }) => {
    const unauth = assertUserAuth(user);
    if (unauth) return unauth;

    try {
        const { model } = await request.json();
        if (!model || typeof model !== 'string') {
            return new Response(JSON.stringify({ error: 'Valid model string is required.' }), {
                status: 400,
                headers: { 'Content-Type': 'application/json' }
            });
        }

        // Validate KV limits (Tutorial section 2: key < 1KB, value < 400KB)
        if (Buffer.byteLength(PREFERRED_MODEL_KEY) > 1024) {
            return new Response(JSON.stringify({ error: 'KEY_TOO_LARGE: Key exceeds 1 KB.' }), { status: 400 });
        }
        if (Buffer.byteLength(model) > 400 * 1024) {
            return new Response(JSON.stringify({ error: 'VALUE_TOO_LARGE: Value exceeds 400 KB.' }), { status: 400 });
        }

        await user.puter.kv.set(PREFERRED_MODEL_KEY, model);

        return new Response(JSON.stringify({ success: true, preferredModel: model }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' }
        });
    } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), {
            status: 500,
            headers: { 'Content-Type': 'application/json' }
        });
    }
});
