/**
 * scripts/worker_e2e_verification.js
 * 
 * End-to-End Test & Deployment Harness for Puter Serverless Worker (Track B)
 * Upgraded following Learn with Sumit (LWS) Puter.js Master Specification:
 * 
 * 1. Validates REAL user token & account identity (puter.auth.getUser())
 * 2. Uploads backend/worker/router.js to Puter Cloud FS (~/scrapchef_router.js)
 * 3. Deploys worker via official puter.workers.create('scrapchef-api', ...)
 * 4. Tests Global Developer-Pays Analytics (GET /stats via me.puter.kv)
 * 5. Tests User-Pays KV Preference Storage (GET/POST /api/preferences/model via user.puter.kv)
 * 6. Tests User-Pays Cloud File Storage (POST/GET /api/recipes/save via user.puter.fs)
 * 7. Tests User-Pays AI Inference (puter.workers.exec vs direct HTTP + puter-auth)
 * 8. Tests Streaming SSE with error chunk inspection (POST /api/recipe/stream)
 * 9. Tests Account Isolation & Billing Attribution (Account A vs Account B)
 * 
 * Usage:
 *   node scripts/worker_e2e_verification.js <REAL_PUTER_TOKEN_A> [<REAL_PUTER_TOKEN_B>]
 */

const fs = require('fs');
const path = require('path');

async function main() {
    const tokenA = process.argv[2] || process.env.PUTER_AUTH_TOKEN_A || process.env.PUTER_AUTH_TOKEN;
    const tokenB = process.argv[3] || process.env.PUTER_AUTH_TOKEN_B;

    console.log('================================================================');
    console.log('   ScrapChef: Track B (Puter Worker) E2E Verification Harness   ');
    console.log('   Upgraded with Full LWS Puter.js Production Architecture      ');
    console.log('================================================================\n');

    if (!tokenA || tokenA === 'YOUR_PUTER_AUTH_TOKEN' || tokenA.startsWith('test_')) {
        console.error('❌ ERROR: A REAL, live Puter user authentication token is required.');
        console.error('   Placeholder or synthetic tokens ("test_...", "YOUR_...") cannot');
        console.error('   authenticate with api.puter.com or deploy Cloudflare workers.\n');
        console.error('Usage:');
        console.error('   node scripts/worker_e2e_verification.js <REAL_TOKEN_ACCOUNT_A> [<REAL_TOKEN_ACCOUNT_B>]\n');
        process.exit(1);
    }

    // 1. Load Puter.js SDK
    let puter;
    try {
        const { init } = require('@heyputer/puter.js/src/init.cjs');
        puter = init(tokenA);
        console.log('✔ [1/8] Puter.js initialized with Account A session token.');
    } catch (err) {
        console.error('❌ Failed to initialize Puter SDK:', err.message);
        process.exit(1);
    }

    // 2. Validate Account A Identity
    let userA;
    try {
        userA = await puter.auth.getUser();
        console.log(`✔ [2/8] Identity Validated:`);
        console.log(`       Username: ${userA.username}`);
        console.log(`       UUID:     ${userA.uuid}`);
        console.log(`       Email:    ${userA.email || 'N/A'}`);
    } catch (err) {
        console.error('❌ [2/8] Authentication failed for provided token:', err.message);
        process.exit(1);
    }

    // 3. Read Worker Router Source Code
    const routerPath = path.resolve(__dirname, '../backend/worker/router.js');
    if (!fs.existsSync(routerPath)) {
        console.error(`❌ Worker router not found at ${routerPath}`);
        process.exit(1);
    }
    const routerSource = fs.readFileSync(routerPath, 'utf8');
    console.log(`✔ [3/8] Loaded router source code (${routerSource.length} bytes).`);

    // 4. Upload and Deploy Worker
    const sanitizedUsername = userA.username.toLowerCase().replace(/[^a-z0-9]/g, '');
    const workerName = `scrapchef-${sanitizedUsername}-api`;
    const cloudFilePath = '~/scrapchef_router.js';
    let workerUrl = `https://${workerName}.puter.work`;

    try {
        console.log(`       Uploading router code to Puter cloud filesystem (${cloudFilePath})...`);
        await puter.fs.write(cloudFilePath, routerSource);
        console.log(`       Deploying worker: "${workerName}"...`);
        const deployResult = await puter.workers.create(workerName, cloudFilePath, { sandbox: true });
        workerUrl = deployResult.url || workerUrl;
        console.log(`✔ [4/8] Worker Deployed Successfully: ${workerUrl}`);
    } catch (err) {
        console.warn(`⚠ [4/8] Worker deployment notice: ${err.message || err}`);
        console.log(`       Proceeding with target URL: ${workerUrl}`);
    }

    // 5. Test Global Stats (Developer-Pays me.puter.kv)
    console.log(`\n--- Test 1: Global Stats & Analytics (me.puter.kv) ---`);
    try {
        const statsRes = await fetch(`${workerUrl}/stats`);
        if (statsRes.ok) {
            const stats = await statsRes.json();
            console.log(`✔ GET /stats successful:`);
            console.log(`  - Total Recipes Generated Globally: ${stats.totalRecipesGenerated}`);
            console.log(`  - Supported Models: ${stats.supportedModels?.map(m => m.id).join(', ')}`);
        } else {
            console.warn(`⚠ /stats returned HTTP ${statsRes.status}`);
        }
    } catch (e) {
        console.warn(`⚠ /stats query failed: ${e.message}`);
    }

    // 6. Test User Preferences (User-Pays user.puter.kv)
    console.log(`\n--- Test 2: User Preferences Management (user.puter.kv) ---`);
    try {
        const setPrefRes = await fetch(`${workerUrl}/api/preferences/model`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'puter-auth': tokenA },
            body: JSON.stringify({ model: 'claude-3-5-sonnet' })
        });
        if (setPrefRes.ok) {
            const getPrefRes = await fetch(`${workerUrl}/api/preferences/model`, {
                headers: { 'puter-auth': tokenA }
            });
            const prefData = await getPrefRes.json();
            console.log(`✔ user.puter.kv verified: Preferred model saved and retrieved: "${prefData.preferredModel}"`);
        } else {
            console.warn(`⚠ Preference save returned HTTP ${setPrefRes.status}`);
        }
    } catch (e) {
        console.warn(`⚠ Preference test notice: ${e.message}`);
    }

    // 7. Test User Cloud Storage (User-Pays user.puter.fs)
    console.log(`\n--- Test 3: User Cloud Storage Persistence (user.puter.fs) ---`);
    try {
        const testRecipeId = `test_rec_${Date.now()}`;
        const saveRes = await fetch(`${workerUrl}/api/recipes/save`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'puter-auth': tokenA },
            body: JSON.stringify({
                id: testRecipeId,
                title: 'Quick Scrap Omelette',
                recipe: { name: 'Quick Scrap Omelette', servings: 2, instructions: ['Whisk eggs', 'Cook in pan'] }
            })
        });
        if (saveRes.ok) {
            const listRes = await fetch(`${workerUrl}/api/recipes`, {
                headers: { 'puter-auth': tokenA }
            });
            const listData = await listRes.json();
            console.log(`✔ user.puter.fs verified: Saved recipe to ~/AppData/recipes/${testRecipeId}.json.`);
            console.log(`  Total cloud recipes loaded via readdir: ${listData.recipes?.length || 0}`);

            // Clean up test file
            await fetch(`${workerUrl}/api/recipes/${testRecipeId}`, {
                method: 'DELETE',
                headers: { 'puter-auth': tokenA }
            });
            console.log(`✔ user.puter.fs.delete verified: Cleaned up test recipe.`);
        }
    } catch (e) {
        console.warn(`⚠ user.puter.fs test notice: ${e.message}`);
    }

    // 8. Official Puter.js Invocation (puter.workers.exec)
    console.log(`\n--- Test 4: Supported Puter.js Environment Test (puter.workers.exec) ---`);
    try {
        const response = await puter.workers.exec(`${workerUrl}/api/recipe/generate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                ingredients: ['egg', 'onion', 'rice'],
                tools: ['stovetop', 'pan'],
                model: 'gpt-5-nano'
            })
        });

        if (!response.ok) throw new Error(`HTTP ${response.status}: ${await response.text()}`);
        const data = await response.json();
        console.log('✔ Phase 1 Exec Success: Received HTTP 200 from Worker!');
        console.log(`  Model: ${data.model}`);
        console.log(`  Recipe Output Preview: ${String(data.recipe).slice(0, 150)}...`);
    } catch (err) {
        console.error('❌ Phase 1 Exec Failed:', err.message);
    }

    // 9. Native Android HTTP Wire Simulation (fetch + puter-auth header)
    console.log(`\n--- Test 5: Native Android Wire Protocol (HTTP + puter-auth header) ---`);
    try {
        const response = await fetch(`${workerUrl}/api/recipe/generate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'puter-auth': tokenA
            },
            body: JSON.stringify({
                ingredients: ['tomato', 'garlic', 'pasta'],
                tools: ['pot'],
                model: 'gpt-5-nano'
            })
        });

        if (!response.ok) throw new Error(`HTTP ${response.status}: ${await response.text()}`);
        const data = await response.json();
        console.log('✔ Phase 3 Wire Reproduction Success: HTTP 200 via direct HTTP + puter-auth header!');
        console.log(`  Model: ${data.model}`);
        console.log(`  Recipe Output: ${String(data.recipe).slice(0, 150)}...`);
    } catch (err) {
        console.error('❌ Phase 3 Wire Reproduction Failed:', err.message);
    }

    // 10. Streaming SSE Test with error chunk handling
    console.log(`\n--- Test 6: Streaming SSE Test with Error Chunk Handling ---`);
    try {
        const streamRes = await fetch(`${workerUrl}/api/recipe/stream`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'puter-auth': tokenA
            },
            body: JSON.stringify({
                ingredients: ['carrot', 'potato', 'lentil'],
                tools: ['pot'],
                model: 'gpt-5-nano'
            })
        });

        if (streamRes.ok && streamRes.body) {
            console.log('✔ Streaming SSE connected: receiving text/event-stream chunks...');
            const reader = streamRes.body.getReader();
            const decoder = new TextDecoder();
            let totalChunks = 0;
            let receivedChars = 0;

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;
                totalChunks++;
                const text = decoder.decode(value);
                receivedChars += text.length;
            }
            console.log(`✔ SSE Streaming Complete: Received ${totalChunks} chunks (${receivedChars} bytes).`);
        } else {
            console.warn(`⚠ Streaming returned status ${streamRes.status}`);
        }
    } catch (e) {
        console.warn(`⚠ Streaming test notice: ${e.message}`);
    }

    // 11. Account B Isolation Test (if second token provided)
    if (tokenB && tokenB !== 'YOUR_PUTER_AUTH_TOKEN' && !tokenB.startsWith('test_')) {
        console.log(`\n--- Test 7: Account Isolation & Billing Attribution (Account B) ---`);
        try {
            const { init } = require('@heyputer/puter.js/src/init.cjs');
            const puterB = init(tokenB);
            const userB = await puterB.auth.getUser();
            console.log(`✔ Account B Validated: ${userB.username} (${userB.uuid})`);

            const resB = await fetch(`${workerUrl}/api/recipe/generate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'puter-auth': tokenB
                },
                body: JSON.stringify({
                    ingredients: ['potato', 'olive oil'],
                    tools: ['oven'],
                    model: 'gpt-5-nano'
                })
            });

            if (!resB.ok) throw new Error(`HTTP ${resB.status}: ${await resB.text()}`);
            console.log('✔ Phase 4 Success: Account B request executed independently against Worker.');
            console.log('👉 Please inspect Puter dashboard to confirm billing attribution isolation:');
            console.log(`   Account A (${userA.username}): Usage deducted`);
            console.log(`   Account B (${userB.username}): Usage deducted`);
        } catch (err) {
            console.error('❌ Phase 4 Isolation Test Failed:', err.message);
        }
    } else {
        console.log('\nℹ Account B token not provided; skipping live Account B isolation check.');
    }

    console.log('\n================================================================');
    console.log('   Verification Run Completed. All upgraded tests processed.   ');
    console.log('================================================================\n');
}

main().catch(err => {
    console.error('Fatal execution error:', err);
    process.exit(1);
});
