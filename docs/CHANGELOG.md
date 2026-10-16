# Changelog

All notable changes to the ScrapChef project will be documented in this file.

## [Unreleased] - 2026-09-10

### Migrated
- **AI Infrastructure Migration:** Complete migration from direct OpenRouter API to Puter AI (`@heyputer/puter.js` wire adapter).
- **Default AI Model:** Migrated default model from `google/gemma-4-26b-a4b-it:free` to `gpt-5-nano` via Puter AI.
- **Per-User Authentication & Keystore Security:** Implemented `AndroidKeystoreTokenEncryptor` (AES-256-GCM) with DataStore integration to encrypt user tokens at rest.
- **Account Management UI:** Added Puter account connection, external browser login intent, and account disconnection in `ProfileScreen`.
- **Repository Architecture:** Replaced `OpenRouterRepository` and `OpenRouterApiService` with `PuterRepository` and `PuterApiService`.
- **Serverless Worker Architecture PoC:** Created `backend/worker/router.js` demonstrating caller context (`user.puter.ai.chat()`) and SSE streaming.
- **Test Infrastructure:** Replaced `MockOpenRouterApiService` with `MockPuterApiService`; updated `RecipeViewModelTest` to test Puter auth, quota, rate-limit, and timeout behaviors (13/13 tests passing).
- **Configuration & Secrets:** Replaced `OPENROUTER_API_KEY` and `OPENROUTER_MODEL` with `PUTER_AUTH_TOKEN` and `PUTER_MODEL` in `.env` and `.env.example`.
- **ProGuard / R8:** Added keep rules for Puter driver DTOs, ViewModels, Room database/DAOs, and typed exceptions; verified minified release APK on physical device.
- **Puter.js Architecture Upgrade (Learn with Sumit Tutorial Integration):**
  - **Worker Modernization (`backend/worker/router.js`):** Added Developer-Pays global recipe analytics with atomic `me.puter.kv.incr()`, `GET /stats` endpoint, stream error chunk inspection (`chunk.type === 'error'`), User-Pays sandboxed cloud recipe storage via `user.puter.fs` (`POST /api/recipes/save`, `GET /api/recipes`, `DELETE /api/recipes/:id`), and User KV preferences (`GET/POST /api/preferences/model`) with key < 1KB, value < 400KB limit validation.
  - **Verification Harness (`scripts/worker_e2e_verification.js`):** Upgraded automated verification harness to test all LWS Puter.js endpoints (stats, preferences, cloud FS, AI streaming, and billing isolation).
  - **Android Reliability (`RecipeViewModel.kt`):** Applied tutorial Bug 5 fix by guaranteeing loading state reset in a `finally` block to prevent UI button freeze on unexpected collection termination.
  - **Puter Web Companion (`web/`):** Created a zero-server-cost React + Vite Web application (`strictPort: true` on port 5173) featuring Puter auth with popup safety, Puter FS cloud storage with debounced non-redundant payload caching, Puter KV model preference, and Puter AI streaming with functional updates to eliminate stale closure race conditions.

### Removed
- Removed `OpenRouterApiService.kt`, `OpenRouterRepository.kt`, and `MockOpenRouterApiService.kt`.
- Removed all active references to `openrouter.ai` and `OPENROUTER_API_KEY`.
