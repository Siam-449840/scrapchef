# ScrapChef Puter Worker (Architecture Option B)

## Overview
This serverless worker runs inside Puter's first-class JavaScript runtime and implements the complete **Puter.js User-Pays Model** and **Stateless Architecture** based on the Learn with Sumit (LWS) Puter.js Master Specification:

1. **Developer-Pays Global Metrics (`me.puter.kv`):** Atomic increment of global recipes generated (`me.puter.kv.incr`).
2. **User-Pays AI Inference (`user.puter.ai`):** User quota attribution via `puter-auth: <token>` header with explicit stream error chunk inspection (`chunk.type === 'error'`).
3. **User Sandboxed Cloud Storage (`user.puter.fs`):** Storing recipes under `~/AppData/<app_id>/recipes/<id>.json` with `{ createMissingParents: true }`.
4. **User Key-Value Preferences (`user.puter.kv`):** Storing model preferences (`gpt-5-nano`, `claude-3-5-sonnet`, `gemini-2.5-flash`) with KV limit validation (< 1 KB key, < 400 KB value).

## Deployment via Puter CLI / Harness
1. Deploy using the automated harness:
   ```bash
   node scripts/worker_e2e_verification.js <REAL_PUTER_TOKEN>
   ```
2. Or via Puter SDK:
   ```javascript
   const puter = require('@heyputer/puter.js');
   await puter.fs.write('~/scrapchef_router.js', fs.readFileSync('backend/worker/router.js', 'utf8'));
   await puter.workers.create('scrapchef-api', '~/scrapchef_router.js', { sandbox: true });
   ```
3. Endpoint URL:
   `https://scrapchef-api.puter.work`

## API Contract

### Global & Health Endpoints
- `GET /health` -> `{ "status": "ok", "service": "scrapchef-puter-worker" }`
- `GET /stats` -> `{ "status": "ok", "totalRecipesGenerated": 42, "supportedModels": [...] }`

### User AI Endpoints (Requires `puter-auth: <token>` header)
- `POST /api/recipe/generate` -> Unary JSON response
  - Body: `{ "ingredients": ["egg", "rice"], "tools": ["stovetop"], "model": "gpt-5-nano" }`
- `POST /api/recipe/stream` -> Server-Sent Events (`text/event-stream`)
  - Chunks: `data: {"text":"..."}\n\n`
  - Termination: `data: [DONE]\n\n`

### User Cloud Storage Endpoints (`user.puter.fs`)
- `POST /api/recipes/save` -> Saves recipe to `~/AppData/<app_id>/recipes/<id>.json`
  - Body: `{ "id": "rec_123", "title": "Egg Fried Rice", "recipe": {...} }`
- `GET /api/recipes` -> Lists all saved recipes from user cloud storage
- `DELETE /api/recipes/:id` -> Deletes recipe from user cloud storage

### User Preferences Endpoints (`user.puter.kv`)
- `GET /api/preferences/model` -> `{ "preferredModel": "gpt-5-nano" }`
- `POST /api/preferences/model` -> `{ "model": "claude-3-5-sonnet" }` (Validates size < 1KB key, < 400KB value)
