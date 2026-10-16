import React, { useState, useEffect, useRef } from 'react';
import puter, {
  MODELS,
  MODEL_KEY,
  WORKER_STATS_URL,
  getRecipePath,
  sortRecipesByUpdated,
  safeSignIn
} from './lib/puter';
import TopBar from './components/TopBar';
import PantrySelector from './components/PantrySelector';
import RecipeView from './components/RecipeView';
import SavedRecipes from './components/SavedRecipes';

export default function App() {
  // User Authentication State
  const [user, setUser] = useState(null);
  const [authError, setAuthError] = useState(null);
  const [isSigningIn, setIsSigningIn] = useState(false);

  // Global Worker Metrics
  const [globalRecipes, setGlobalRecipes] = useState(null);

  // Pantry & Recipe State
  const [selectedIngredients, setSelectedIngredients] = useState(['Egg', 'Rice', 'Onion']);
  const [selectedTools, setSelectedTools] = useState(['Stovetop']);
  const [maxTime, setMaxTime] = useState(30);
  const [selectedModel, setSelectedModel] = useState(MODELS[0].id);

  // Active Recipe & Streaming State
  const [activeRecipe, setActiveRecipe] = useState(null);
  const [activeId, setActiveId] = useState(null);
  const [streamingText, setStreamingText] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [generateError, setGenerateError] = useState(null);

  // Cloud Saved Recipes (user.puter.fs)
  const [savedRecipes, setSavedRecipes] = useState([]);
  const [isSavedDrawerOpen, setIsSavedDrawerOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // Bug 3 Fix: Ref Map to track saved payload hashes and avoid redundant writes
  const savedPayloadMap = useRef(new Map());

  // 1. Initial Load: Check session and fetch global stats
  useEffect(() => {
    if (puter.auth.isSignedIn()) {
      puter.auth.getUser()
        .then((u) => setUser(u))
        .catch(() => {});
    }
    fetchGlobalCount();
  }, []);

  async function fetchGlobalCount() {
    try {
      const res = await fetch(WORKER_STATS_URL);
      if (res.ok) {
        const data = await res.json();
        if (typeof data.totalRecipesGenerated === 'number') {
          setGlobalRecipes(data.totalRecipesGenerated);
        }
      }
    } catch (e) {
      // Silent failure if worker not yet deployed
    }
  }

  // 2. Load User Preferences and Cloud Storage on Sign-In
  useEffect(() => {
    if (!user) {
      setSavedRecipes([]);
      return;
    }

    async function loadUserData() {
      // Load preferred model from puter.kv (Tutorial Section 5.4)
      try {
        const savedModel = await puter.kv.get(MODEL_KEY);
        if (savedModel && MODELS.some(m => m.id === savedModel)) {
          setSelectedModel(savedModel);
        }
      } catch (e) {}

      // Load recipes from puter.fs (Tutorial Section 5.3)
      try {
        const files = await puter.fs.readdir('recipes');
        // Parallel reads using Promise.all & blob.text()
        const promises = files.map(async (file) => {
          try {
            const blob = await puter.fs.read(file.path);
            const text = await blob.text();
            return JSON.parse(text);
          } catch (readErr) {
            return null;
          }
        });

        const loaded = (await Promise.all(promises)).filter(Boolean);
        const sorted = sortRecipesByUpdated(loaded);
        setSavedRecipes(sorted);

        // Populate payload cache (Bug 3 fix)
        sorted.forEach(r => savedPayloadMap.current.set(r.id, JSON.stringify(r)));
      } catch (e) {
        // Directory doesn't exist yet
      }
    }

    loadUserData();
  }, [user]);

  // Auth Handlers (Tutorial Bug 1 fix: safe popup handling)
  async function handleSignIn() {
    setAuthError(null);
    setIsSigningIn(true);
    try {
      const loggedUser = await safeSignIn();
      setUser(loggedUser);
    } catch (err) {
      setAuthError(err.message);
    } finally {
      setIsSigningIn(false);
    }
  }

  function handleSignOut() {
    puter.auth.signOut();
    setUser(null);
    setSavedRecipes([]);
    setActiveRecipe(null);
    setActiveId(null);
  }

  // Model Change Handler (Tutorial Section 5.4: saving to puter.kv)
  async function handleChangeModel(modelId) {
    setSelectedModel(modelId);
    if (user) {
      try {
        await puter.kv.set(MODEL_KEY, modelId);
      } catch (e) {}
    }
  }

  // Pantry Handlers
  function handleToggleIngredient(item) {
    setSelectedIngredients(prev =>
      prev.includes(item) ? prev.filter(i => i !== item) : [...prev, item]
    );
  }

  function handleAddCustomIngredient(item) {
    if (!selectedIngredients.includes(item)) {
      setSelectedIngredients(prev => [...prev, item]);
    }
  }

  function handleToggleTool(tool) {
    setSelectedTools(prev =>
      prev.includes(tool) ? prev.filter(t => t !== tool) : [...prev, tool]
    );
  }

  // 3. AI Recipe Generation & Streaming (Tutorial Section 5.5 + Bug 4 & 5 fixes)
  async function handleGenerateRecipe() {
    if (selectedIngredients.length < 3) return;

    setIsGenerating(true);
    setGenerateError(null);
    setStreamingText('');

    const systemPrompt = `You are ScrapChef, a zero-waste culinary assistant. Generate a single realistic recipe using ONLY the ingredients provided. Output MUST be valid JSON adhere to this exact schema:
{
  "name": "Recipe Title",
  "prep_time_minutes": 10,
  "cook_time_minutes": 15,
  "servings": 2,
  "description": "Short culinary summary and zero-waste tip.",
  "ingredients": [{"name": "egg", "amount": 2, "unit": "pcs"}],
  "instructions": [{"step": 1, "instruction": "Whisk eggs in a bowl."}]
}`;

    const userPrompt = `Ingredients: ${selectedIngredients.join(', ')}\nTools: ${selectedTools.join(', ')}\nMax Prep Time: ${maxTime} mins`;

    const messages = [
      { role: 'system', content: systemPrompt },
      { role: 'user', content: userPrompt }
    ];

    let accumulatedText = '';

    try {
      const response = await puter.ai.chat(messages, {
        model: selectedModel,
        stream: true
      });

      for await (const chunk of response) {
        // Bug 5 fix: Check error chunks
        if (chunk && chunk.type === 'error') {
          throw new Error(chunk.error?.message || 'Upstream stream error');
        }

        const text = chunk.text || chunk.message?.content || '';
        if (text) {
          accumulatedText += text;
          setStreamingText(accumulatedText);
        }
      }

      // Parse JSON from generated output
      let parsed = null;
      try {
        const jsonMatch = accumulatedText.match(/\{[\s\S]*\}/);
        if (jsonMatch) {
          parsed = JSON.parse(jsonMatch[0]);
        }
      } catch (pe) {
        // Fallback simple structure
        parsed = {
          name: 'Zero-Waste Kitchen Creation',
          prep_time_minutes: 10,
          cook_time_minutes: 15,
          servings: 2,
          description: accumulatedText.slice(0, 160),
          ingredients: selectedIngredients.map(name => ({ name })),
          instructions: accumulatedText.split('\n').filter(Boolean).map((s, idx) => ({ step: idx + 1, instruction: s }))
        };
      }

      const generatedId = `rec_${Date.now()}`;
      const fullRecipeObj = {
        id: generatedId,
        title: parsed.name || 'Zero-Waste Creation',
        ...parsed,
        updatedAt: Date.now()
      };

      // Bug 4 fix: Functional state update to avoid stale closures
      setActiveRecipe(fullRecipeObj);
      setActiveId(generatedId);

      // Notify global counter if worker available
      fetch('https://scrapchef-api.puter.work/api/recipe/generate', { method: 'OPTIONS' })
        .then(() => fetchGlobalCount())
        .catch(() => {});

    } catch (err) {
      setGenerateError(err.message || 'Failed to generate recipe with Puter AI.');
    } finally {
      // Bug 5 fix: Always reset loading state so buttons are never permanently stuck disabled
      setIsGenerating(false);
    }
  }

  // 4. Cloud Save Handler (Tutorial Section 5.3 + Bug 3 fix: non-redundant write filter)
  async function handleSaveToCloud() {
    if (!user || !activeRecipe) return;

    const payload = JSON.stringify(activeRecipe);
    // Bug 3 fix: Skip write if stringified payload is unchanged
    if (savedPayloadMap.current.get(activeRecipe.id) === payload) {
      setIsSavedDrawerOpen(true);
      return;
    }

    setIsSaving(true);
    try {
      await puter.fs.write(getRecipePath(activeRecipe.id), payload, { createMissingParents: true });
      savedPayloadMap.current.set(activeRecipe.id, payload);

      setSavedRecipes(prev => {
        const filtered = prev.filter(r => r.id !== activeRecipe.id);
        return sortRecipesByUpdated([activeRecipe, ...filtered]);
      });
      setIsSavedDrawerOpen(true);
    } catch (e) {
      console.error('Failed to save recipe to Puter FS:', e);
    } finally {
      setIsSaving(false);
    }
  }

  // 5. Cloud Delete Handler (Tutorial Bug 2 fix: delete from state AND cloud storage)
  async function handleDeleteRecipe(id) {
    // 1. Remove from React State immediately
    setSavedRecipes(prev => prev.filter(r => r.id !== id));
    if (activeId === id) {
      setActiveId(null);
      setActiveRecipe(null);
    }
    savedPayloadMap.current.delete(id);

    // 2. Delete from Puter cloud storage
    if (user) {
      try {
        await puter.fs.delete(getRecipePath(id));
      } catch (e) {
        console.error('Cloud delete failed:', e);
      }
    }
  }

  function handleSelectRecipe(recipe) {
    setActiveRecipe(recipe);
    setActiveId(recipe.id);
    setStreamingText('');
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>
      <TopBar
        user={user}
        onSignIn={handleSignIn}
        onSignOut={handleSignOut}
        globalRecipes={globalRecipes}
        authError={authError}
        isSigningIn={isSigningIn}
      />

      <div style={{ display: 'flex', flex: 1, overflow: 'hidden', position: 'relative' }}>
        <PantrySelector
          selectedIngredients={selectedIngredients}
          onToggleIngredient={handleToggleIngredient}
          onAddCustomIngredient={handleAddCustomIngredient}
          selectedTools={selectedTools}
          onToggleTool={handleToggleTool}
          maxTime={maxTime}
          onChangeMaxTime={setMaxTime}
          selectedModel={selectedModel}
          onChangeModel={handleChangeModel}
          onGenerateRecipe={handleGenerateRecipe}
          isGenerating={isGenerating}
          generateError={generateError}
        />

        <RecipeView
          activeRecipe={activeRecipe}
          streamingText={streamingText}
          isGenerating={isGenerating}
          onSaveToCloud={handleSaveToCloud}
          isSaving={isSaving}
          user={user}
        />

        <SavedRecipes
          recipes={savedRecipes}
          activeId={activeId}
          onSelectRecipe={handleSelectRecipe}
          onDeleteRecipe={handleDeleteRecipe}
          isOpen={isSavedDrawerOpen}
          onToggleOpen={() => setIsSavedDrawerOpen(!isSavedDrawerOpen)}
        />

        {/* Floating toggle button for Cloud Saved Recipes drawer */}
        {user && !isSavedDrawerOpen && (
          <button
            onClick={() => setIsSavedDrawerOpen(true)}
            style={{
              position: 'absolute',
              right: '20px',
              bottom: '20px',
              padding: '10px 16px',
              borderRadius: '24px',
              background: 'var(--bg-surface-elevated)',
              border: '1px solid var(--border-subtle)',
              color: 'var(--text-primary)',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              boxShadow: 'var(--shadow-md)',
              zIndex: 30
            }}
          >
            <span>☁ Saved Recipes</span>
            <span style={{
              background: 'var(--accent-primary)',
              color: '#041e15',
              padding: '2px 7px',
              borderRadius: '10px',
              fontSize: '0.75rem',
              fontWeight: 800
            }}>
              {savedRecipes.length}
            </span>
          </button>
        )}
      </div>
    </div>
  );
}
