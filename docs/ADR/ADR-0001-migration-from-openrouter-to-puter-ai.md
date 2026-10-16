# ADR 0001: Migration from OpenRouter to Puter AI Infrastructure (Native Client Wire Adapter)

## Status
Accepted — Dual-Track Architecture:
- **Track A (Current Native Wire Adapter):** Hardened, Keystore-backed, on-device verified. Classified as **Experimental Fallback / Diagnostic Implementation**.
- **Track B (Puter Serverless Worker Bridge):** PoC authored (`backend/worker/router.js`). Selected as **Preferred Production Target Candidate** (pending live account E2E deployment & billing attribution).

## Context
ScrapChef previously relied directly on OpenRouter's HTTP chat completions API (`https://openrouter.ai/api/v1/chat/completions`) for recipe generation. This created developer-side billing overhead, vendor coupling, and required shared API keys.

Puter (`@heyputer/puter.js`) provides an AI ecosystem with a **User-Pays Model**, 900+ multi-model catalog access (OpenAI, Anthropic, Google, Mistral, Meta), and developer/user isolation.

### Technical & Architectural Challenges
1. **Official Platform Support vs. Android Native:**
   - Official Puter.js documentation explicitly lists supported platforms: **Websites, Puter Apps, Node.js, and Serverless Workers**.
   - There is **no official Puter Android/Kotlin SDK**.
   - Therefore, two architectural approaches were developed and evaluated:
     - **Track A: Native Client Wire Adapter:** Replicate the exact HTTP wire protocol executed by `@heyputer/puter.js` v2.6.2 (`POST https://api.puter.com/drivers/call` with `driver: "ai-chat"` and `method: "complete"`).
     - **Track B: Puter Serverless Worker Bridge:** Deploy a Puter Worker that calls `user.puter.ai.chat(...)` and invoke it via HTTP with `puter-auth: <token>`. This aligns directly with official Puter serverless backend primitives.
2. **Wire Protocol Stability & Classification:**
   - Source code analysis reveals `puter.workers.exec()` over the wire is simply standard HTTP `fetch()` with a `puter-auth: <token>` header, which populates `user.puter` on Puter's cloud gateway.
   - However, calling a Puter Worker does not eliminate the native Android authentication challenge; the native app must still obtain and securely store the user's credential.
   - `/drivers/call` is classified as an **Internal Implementation Detail / Client Wire Transport**, NOT a guaranteed public REST contract.
3. **Security & User-Pays Compliance:**
   - In native Android, hardcoding a shared developer token inside the APK violates Puter's User-Pays model.
   - Credentials at rest must be protected; plain text in DataStore is vulnerable.
4. **Prompt Engineering & UX Preservation:**
   - ScrapChef's core value is its strict recipe prompt engineering, JSON schema validation, pantry-gap analysis, and real-time streaming feedback.

## Decision
1. **Retain Native Client Wire Adapter as Experimental Fallback (Track A):**
   - Implemented `PuterApiService` and `PuterRepository` calling `https://api.puter.com/drivers/call`.
   - Tuned for defensive NDJSON / SSE stream consumption and error mapping.
2. **Establish Puter Serverless Worker Bridge as Production Target (Track B):**
   - Authored `backend/worker/router.js` using official `user.puter.ai.chat()`.
   - Allows prompt engineering, model selection, and token limits to be governed server-side without releasing new APKs.
3. **User Authentication Lifecycle & UI:**
   - Added a dedicated **AI Engine & Account (Puter)** section in [`ProfileScreen.kt`](file:///Users/ishtiaqueibnmalek/Downloads/scrapchef/app/src/main/java/com/scrapchef/android/ui/screens/ProfileScreen.kt).
   - Users can connect their personal account via token, launch `https://puter.com/` directly, or disconnect/switch accounts (`clearPuterAuthToken()`).
4. **Hardware-Backed Credential Security (`AndroidKeystoreTokenEncryptor`):**
   - Implemented [`AndroidKeystoreTokenEncryptor`](file:///Users/ishtiaqueibnmalek/Downloads/scrapchef/app/src/main/java/com/scrapchef/android/util/TokenEncryptor.kt) using AES-256-GCM backed by `AndroidKeyStore`.
   - Verified on physical device: DataStore stores only ciphertext and IV; plaintext credentials never touch disk.
   - In release builds, `PUTER_AUTH_TOKEN` in `BuildConfig` remains empty; each user supplies their own credential.
5. **Dual Delivery (Unary + Defensive Streaming):**
   - Provide standard unary `generateRecipe()` parsing JSON via Moshi.
   - Provide `generateRecipeStream()` parsing chunked NDJSON / SSE streams defensively, preserving instant UI feedback in `StreamingRecipeParser`.
6. **Physical Hardware & Release Build Verification:**
   - Validated on physical device (`CPH2709`, Android 16): cold start, KeyStore encryption, UI dialogs, and 401 error interception.
   - Validated minified release APK (`app-release.apk`, 2.75 MB) with R8 keep rules; binary strings audited 100% clean of credentials.

## Consequences
- **Positive:**
  - Zero OpenRouter dependency surface remains in the codebase.
  - Multi-model flexibility through Puter catalog (934 models verified).
  - Per-user encrypted credentials align with User-Pays billing architecture.
  - 100% prompt engineering, dietary rules, and recipe validation preserved.
  - Full test coverage passing (13/13 unit tests) and clean release build.
- **Operational Considerations / Risks:**
  - Puter does not provide an official native Kotlin Android SDK.
  - Track A (`drivers/call`) is an internal transport; retained strictly as a diagnostic/experimental fallback.
  - Track B (Serverless Worker) is the preferred production path; final production sign-off requires live account E2E verification (HTTP 200 + Account A/B billing isolation).

## On-Device Smoke Test Runbook (When Physical Device is Attached)
1. Install debug APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
2. Launch ScrapChef: `adb shell am start -n com.scrapchef.android/.MainActivity`.
3. Open Profile tab -> Tap "Puter AI Account" -> Paste personal Puter token -> Tap "Save".
4. Navigate to Pantry -> Select 3+ ingredients (e.g. Tomato, Onion, Garlic) -> Select Stovetop -> Tap "Cook something".
5. Observe streaming generation in Kitchen Mode -> Verify generated recipe instructions and pantry assumptions.
6. Return to Profile -> Tap "Disconnect Current Account" -> Verify that subsequent recipe generation prompts for Puter connection.
