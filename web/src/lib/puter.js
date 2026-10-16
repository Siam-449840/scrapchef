/**
 * web/src/lib/puter.js
 * Core Puter.js integration library for ScrapChef Web
 * 
 * Follows the Learn with Sumit (LWS) Puter.js Master Specification:
 * - Silence promotional logs: `puter.quiet = true`
 * - Cloud Storage Sandboxing: `recipes/<id>.json` resolving to `~/AppData/<app_id>/recipes/`
 * - Key-Value Store: `preferred_model` with size limit awareness
 * - Safe Auth with popup exception trapping
 */
import puter from '@heyputer/puter.js';

// Silence promotional banner logs at entry point (Tutorial Section 7.5)
if (puter) {
  puter.quiet = true;
}

export const MODELS = [
  { id: 'gpt-5-nano', name: 'GPT-5 Nano (Fast, Cheap & Default)', cost: 'Lowest' },
  { id: 'claude-3-5-sonnet', name: 'Claude 3.5 Sonnet (Nuanced Flavor Profiles)', cost: 'Standard' },
  { id: 'gemini-2.5-flash', name: 'Gemini 2.5 Flash (Rapid Parsing)', cost: 'Low' },
];

export const MODEL_KEY = 'preferred_model';
export const WORKER_STATS_URL = 'https://scrapchef-api.puter.work/stats';

/**
 * Returns formatted file path inside Puter sandboxed AppData:
 * Resolves to ~/AppData/<app_id>/recipes/<id>.json
 */
export function getRecipePath(id) {
  return `recipes/${id}.json`;
}

/**
 * Sorts recipes by most recently updated
 */
export function sortRecipesByUpdated(recipeList) {
  return [...recipeList].sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0));
}

/**
 * Safe authentication wrapper with popup blocked detection (Tutorial Bug 1)
 */
export async function safeSignIn() {
  try {
    await puter.auth.signIn();
    return await puter.auth.getUser();
  } catch (err) {
    const msg = err.message || String(err);
    if (msg.toLowerCase().includes('popup') || msg.toLowerCase().includes('blocked') || msg.toLowerCase().includes('closed')) {
      throw new Error('Sign-in popup was blocked or closed. Please allow popups for this site and try again.');
    }
    throw new Error(`Puter Authentication failed: ${msg}`);
  }
}

export default puter;
