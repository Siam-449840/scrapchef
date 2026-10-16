# ScrapChef Roadmap

## Current Phase: Implementation & Hardening

### Completed Milestones
- [x] Room / DataStore persistence foundation
- [x] MVVM Recipe generation flow & Kitchen Mode UI
- [x] Streaming recipe generation parser
- [x] AI Infrastructure Migration: OpenRouter -> Puter.js / Puter AI (`gpt-5-nano`)
- [x] Puter User-Pays model support (per-user auth in DataStore with Android Keystore encryption)
- [x] Defensive streaming handling (NDJSON / SSE)
- [x] On-device Puter account connection, disconnect, and token entry UI in Profile screen
- [x] Physical device validation on Android 16 (cold start, Keystore-backed AES-256-GCM, release R8 audit)
- [x] Track A Architecture: Hardened client wire adapter retained as experimental/diagnostic fallback
- [x] Track B Architecture: Puter Serverless Worker PoC authored (`backend/worker/router.js`)

### Upcoming Tasks
- [ ] Deploy Puter Serverless Worker to `*.puter.work` and execute live E2E recipe stream
- [ ] End-to-end User-Pays billing attribution test with live Puter accounts (Account A vs Account B)
- [ ] Recipe generation latency benchmarking (Puter `gpt-5-nano` vs. `gemini-3.1-flash-lite`)
- [ ] Offline recipe caching enhancements
