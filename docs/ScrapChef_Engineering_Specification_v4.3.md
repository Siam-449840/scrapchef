# ScrapChef v4.3 Engineering Specification
### The Lean, Implementation-Accurate Build Document — Governance-Hardened Per External Review

| | |
|---|---|
| **Product name** | ScrapChef |
| **Platform** | Native Android (Kotlin, Jetpack Compose) |
| **Document version** | 4.3.0 — Supersedes v4.2.0. Closes out the implementation-ambiguity items raised in external technical review (dependency policy, DI strategy, state ownership, repository contract, naming conventions, version matrix, API versioning, logging/configuration policy, release checklist, and several previously-open design tokens) |
| **Status** | Final — Locked for Implementation |
| **Intended reader** | Any AI coding agent or engineer continuing work on ScrapChef |

---

## Table of Contents
0. Document Control & Supersession Notice
1. Product Vision
2. Engineering Principles
3. Executive Summary
4. Competitive Context (Condensed)
5. Goals & Non-Goals
6. Target Users & Personas
7. Localization & Language Strategy
8. Feature Catalog (Current Scope)
9. Information Architecture & Navigation
10. Design System (Tokens, Icons, Hero Gradients, Motion)
11. Screen-by-Screen UI Specification
12. Component Library
13. Interaction Performance Targets
14. Motion & Microinteractions
15. UX Principles
16. Engineering Standards & Conventions — **expanded in v4.3: dependency policy, configuration policy, naming conventions, version matrix**
17. State Management & Data Flow — **expanded in v4.3: state ownership, repository contract**
18. Backend Architecture
19. AI Architecture
20. API Specification — **expanded in v4.3: API versioning policy**
21. Database Design
22. Auth, Security & Privacy — **expanded in v4.3: logging policy**
23. Performance, Caching & Accessibility
24. Analytics, Testing & CI/CD
25. Business Model & Future Phases
26. Acceptance Criteria & Definition of Done — **expanded in v4.3: release checklist**
27. Deprecated Concepts — Do Not Reintroduce
28. Architecture Decision Records (ADR)
29. Open Questions & Assumptions Log — **trimmed in v4.3**

---

## 0. Document Control & Supersession Notice

**This document replaces the earlier ~200-page ScrapChef blueprint.** That document described an aspirational, much broader product (voice input, photo/fridge scanning, a dedicated Vision AI service, direct Gemini integration, AI-generated hero photography). The actual implementation intentionally narrowed to a leaner, faster MVP, and the old document now actively misleads anyone — human or AI agent — who reads it as current.

**Rules for this document:**
1. If anything here conflicts with the old blueprint, **this document wins.**
2. §27 ("Deprecated Concepts") lists specific features, screens, and class names that no longer exist. Do not reintroduce them, reference them, or pattern-match new work against them.
3. Do not invent features, screens, endpoints, or fields not defined here. Log genuine ambiguity in `DECISIONS.md` rather than guessing (§29).
4. This is still an **online, AI-first** product — there is no offline recipe generation and no bundled static recipe database. That part of the original architecture direction was correct and is unchanged.
5. Numbers (timeouts, limits, retry counts) stated here are exact requirements, not suggestions.
6. v4.2 folded a previously-separate document — the "ScrapChef Complete Design System Reference" — into §10 as the single authoritative source for color, typography, gradient, and motion values. Genuine mismatches between that source and the rest of this spec were logged rather than silently resolved.
7. **New in v4.3:** an external technical review of v4.2 scored it 9.95/10 and identified twelve remaining sources of implementation ambiguity — mostly places where the document deferred a decision to `DECISIONS.md` that it could reasonably have just made. This version closes out nearly all of them directly: motion durations, color-role mapping, dependency policy, DI strategy, repository contracts, API versioning, logging/configuration policy, naming conventions, state ownership, and the toolchain version matrix (see §10, §16, §17, §20, §22, §26, §28). What remains open in §29 is now intentionally short, limited to items that genuinely depend on something outside this team's control — not unresolved design questions.

---

## 1. Product Vision

> **ScrapChef is a minimal, lightning-fast AI recipe generator that transforms a few ingredients into one high-quality recipe in seconds.**

Every section below exists to support that sentence. Where a feature or a piece of polish doesn't clearly serve "fast, minimal, one great recipe," it doesn't belong in the product, regardless of what a previous version of this document said.

---

## 2. Engineering Principles

These govern every decision below, for human engineers and AI coding agents alike. When a section and a principle seem to conflict, treat that as a signal to re-read the section rather than to override the principle.

1. **Simplicity over cleverness.** The obvious implementation beats the elegant one.
2. **Delete instead of abstract whenever possible.** An unused abstraction is worse than duplicated code that's easy to delete later.
3. **One source of truth.** Every piece of state, every design value, every localization setting lives in exactly one place (§10.1, §18.3).
4. **No duplicate business logic.** If two screens need the same rule, it lives in one shared use case, not two copies.
5. **Every feature must justify its complexity.** "It might be useful later" is not a justification — see §4.2 and §25.
6. **Performance first.** When a choice trades speed or reliability for visual richness, the leaner, faster choice wins (§15, §10.3).
7. **Readability over micro-optimizations.** Optimize the hot paths named in §13; don't hand-tune the rest at the cost of clarity.
8. **Prefer composition over inheritance,** in both the Compose component library (§12) and the backend services (§18).
9. **Every new dependency requires justification** against §16.2's allowed list.
10. **If a feature is not documented here, it should not exist.** This is the same rule §0 states for the document as a whole — it's listed here again because it's the principle everything else in this list serves.

---

## 3. Executive Summary

ScrapChef is a native Android app. A user enters the ingredients they have through a fast autocomplete interface, chooses the cooking tools and time they have available, and receives a single, streamed, AI-generated recipe — optimized automatically for their country, language, and dietary preferences, which are set once in Settings and applied to every request without being re-asked. The product's entire value proposition is speed and confidence: one great recipe, not a list to sort through, delivered so quickly the AI generation feels closer to an instant answer than a "please wait" experience.

There is no voice input, no photo/camera ingredient recognition, and no multimodal AI in the current product. These were part of an earlier, broader vision and have been deliberately cut (§27) in favor of doing the ingredients-to-recipe core loop extremely well.

---

## 4. Competitive Context (Condensed)

The full competitive teardown (SuperCook, Samsung Food, Cookpad, ChefGPT) from the earlier blueprint remains valid research and is not repeated here in full. The one-line takeaway that still governs product decisions:

> Every competitor either overwhelms the user with a list of options (SuperCook), demands manual effort to get value (Samsung Food's drag-and-drop planner), lacks AI entirely (Cookpad), or buries a good AI core under feature clutter and multiple modes (ChefGPT).

ScrapChef's answer, unchanged in spirit but now sharper in execution: **one confident recipe, minimal taps, nothing to configure twice.** The "many modes" clutter problem is now avoided even more directly than before — ScrapChef doesn't just organize multiple AI modes cleanly, it has exactly one input flow (§9, §11).

---

## 5. Goals & Non-Goals

### 5.1 Goals
- Turn 3–8 typed ingredients into a streamed, guardrailed AI recipe in seconds.
- Apply country, language, and dietary preferences automatically to every generation via the Settings → AI Prompt Pipeline (§18.3) — never re-asked per request.
- Maintain a strict, centralized design system (design tokens, ingredient icons, hero gradients — §10) so the UI stays fast, consistent, and free of one-off styling decisions.
- Keep the AI stack thin and swappable: one prompt builder, one model-routing layer (OpenRouter), one guardrail pipeline, no per-modality branching.
- Ship a genuinely minimal, uncluttered UI: one home surface, one ingredient-input flow, one recipe result.

### 5.2 Non-Goals (explicitly out of scope for this version)
- Voice input of any kind (dictation, voice commands, hands-free narration) — see §27.
- Photo, camera, or barcode-based ingredient capture, and any form of Vision AI — see §27.
- Offline recipe generation or a bundled static recipe database (unchanged from the prior architecture direction).
- AI-generated or stock hero photography — replaced entirely by deterministic gradients (§10.3).
- Multi-modal AI of any kind. The AI layer accepts and returns structured text/JSON only.
- A social/community layer, meal-plan auto-generation, grocery delivery integration, or smart-appliance integration. These remain plausible future phases (§25) but are not part of this specification and must not be built against it.

---

## 6. Target Users & Personas

Everyone who wants a fast answer to "what can I cook with this?" — the product is intentionally general-purpose rather than niche-tuned, and its speed-and-simplicity bar should read as effortless to a rushed weeknight cook and to someone with no cooking confidence at all, equally.

- **Alex, 25, urban professional.** Has eggs, cheese, a bell pepper, rice. Wants a hot meal in under 15 minutes, typed in during a work break.
- **Maria, 38, parent.** Has broccoli, tofu, soy sauce, ginger. Stovetop, 30 minutes, cares about a recipe that doesn't require a special-trip ingredient.
- **Priya, 29, Mumbai.** Wants recipes that default to her region's pantry staples and cuisine conventions without having to specify this every time — handled entirely by Settings (§18.3), not by anything she types per-request.

These personas are unchanged from the earlier document in substance; what changed is how they reach a recipe — always through the same single typed-ingredient flow, never a mode choice.

---

## 7. Localization & Language Strategy

Localization remains a first-class part of the product and is **unaffected by the voice/photo removal** — it was never tied to input modality.

- **Country** (ISO 3166-1 alpha-2) is set once in onboarding/Settings and drives: default cuisine weighting, the regional pantry-staples list, measurement system default (Metric/Imperial), and ingredient display-name conventions (e.g., "coriander" vs. "cilantro").
- **Language** (BCP-47) is set once and the AI is instructed to respond entirely in that language — recipe name, steps, and tip, not just app chrome.
- Both values live in the user's profile and are injected automatically into every generation request by the Settings → AI Prompt Pipeline (§18.3) — the ingredient-input flow itself carries no localization fields.
- RTL languages (Arabic, Urdu) remain a first-class layout mode: all Compose layouts use `start`/`end`, never `left`/`right`.
- Launch language set is unchanged from the original plan: English, Spanish, Portuguese, French, German, Italian, Dutch, Russian, Turkish, Arabic, Hindi, Bengali, Urdu, Indonesian, Vietnamese, Thai, Chinese (Simplified), Japanese, Korean, Malay.

---

## 8. Feature Catalog (Current Scope)

| Feature | Description |
|---|---|
| **Ingredient input** | Single flow: fast autocomplete search with synonym matching, chip-based selection, 3–8 ingredients |
| **Tool selection** | Multi-select chips: Stovetop, Oven, Microwave, Air Fryer, No-Cook — at least one always selected |
| **Time selection** | Single-choice: 15 / 30 / 45 min / No limit |
| **Pantry integration** | A lightweight, persistent list of ingredients the user has on hand, offered as quick-add suggestions in the input flow |
| **AI recipe generation** | Single streamed recipe per request, guardrailed (§19.6), country/language/dietary-aware automatically |
| **Recipe detail view** | Dish name, time/tools, ingredients (yours vs. pantry staples), numbered steps, one tip |
| **Cook Mode** | Manual, tap/swipe step-through with large type and multi-timer support — no voice (§11.6) |
| **Favorites** | Save/unsave a recipe; one favorite control per recipe, not duplicated across screens |
| **Recent recipes** | Last few generated recipes surfaced on Home for quick re-open |
| **Settings** | Country, language, dietary preferences, measurement system — the single source of truth injected into every AI request |
| **Centralized ingredient icons** | Every ingredient (chip, pantry row, recipe list) renders through one icon-mapping service — no emoji |
| **Design tokens** | Every color, type style, spacing, and radius value comes from one theme source — nothing hardcoded per-screen |
| **Recipe hero gradients** | Every recipe gets one of 7 deterministic gradients, chosen by a stable hash — no photography, no randomness |

Anything not in this table is out of scope for this version (§5.2, §27).

---

## 9. Information Architecture & Navigation

### 9.1 Navigation Graph
```
NavHost(startDestination = "home") {
    composable("onboarding_country_lang") { ... }   // first-launch only
    composable("onboarding_dietary") { ... }
    composable("home") { HomeScreen(...) }
    composable("ingredient_input") { IngredientInputScreen(...) }   // no mode parameter
    composable("generating/{requestId}") { GeneratingScreen(...) }
    composable("recipe/{recipeId}") { RecipeDetailScreen(...) }
    composable("cook_mode/{recipeId}") { CookModeScreen(...) }
    composable("pantry") { PantryScreen(...) }
    composable("pantry/add") { PantryAddScreen(...) }
    composable("favorites") { FavoritesScreen(...) }
    composable("settings") { SettingsScreen(...) }
    composable("settings/dietary") { DietaryPreferencesScreen(...) }
    composable("settings/country_lang") { CountryLanguageScreen(...) }
    composable("error/{errorType}") { ErrorScreen(...) }
}
```
There is no `{mode}` segment anywhere in the graph. Ingredient Input is a single screen with a single behavior — the old `type | voice | photo` branching (§27) does not exist in this version.

### 9.2 Bottom Navigation
Four destinations, unchanged in count from the prior spec, but simpler in content:
1. **Home** — the ingredient-entry launcher (§11.1)
2. **Favorites** — saved recipes
3. **Pantry** — the persistent ingredient list
4. **Settings** — country, language, dietary preferences (folds the old "Profile" naming into the plainer "Settings," since there is no account/subscription surface in this version — see §22)

### 9.3 Back-Stack Behavior
Recipe Detail → back returns to whatever launched generation (Home, Favorites, or Pantry) with prior selections intact. Cook Mode is a modal, full-screen destination with the system back gesture intercepted by an "Exit Cook Mode?" confirmation, unchanged in principle from before — this behavior was never voice-dependent.

---

## 10. Design System

This section documents both the **architectural rules** governing how tokens are structured (§10.1–§10.3) and the **full authoritative token values** themselves (§10.4–§10.7), merged in from the previously-separate "ScrapChef Complete Design System Reference" (v4.2) and now, as of v4.3, with the previously-open motion/color-mapping questions resolved rather than deferred.

**A note on source format.** The concrete values in §10.4–§10.7 were originally authored as CSS custom properties (`oklch(...)`, `globals.css`, `layout.tsx`) for a web/Next.js token reference. ScrapChef's actual client is native Android/Compose (§3, §16.2), so every color below is given with its **OKLCH source value** (for auditing against the original design tool) alongside the **hex/RGB value Android engineering binds directly into `ScrapChefTheme`** as a Compose `Color(0xFF......)`. The hex/RGB values are canonical for this app going forward — if the palette is ever regenerated from OKLCH again, that's a new, deliberate design task at that time, not a standing conversion ambiguity to track here.

### 10.1 Design Token System
- **Single source of truth.** Every color, type style, spacing value, and corner radius used anywhere in the app resolves through one Compose theme object (`ScrapChefTheme` → `colorScheme`, `typography`, `spacing`, `shapes`). Composables reference tokens (`MaterialTheme.colorScheme.primary`, `ScrapChefTheme.spacing.md`, `ScrapChefTheme.shapes.card`) and never a literal hex code, raw `.dp` value, or ad-hoc `RoundedCornerShape(x.dp)`. The values those tokens resolve to are defined in §10.4–§10.6 below, not re-derived per screen.
- **Why this is a hard rule, not a style preference:** a hardcoded value in one screen is invisible to a future theme change (dark mode, dynamic color, a rebrand) and silently drifts from the rest of the app. Centralizing it is what keeps "every screen follows the design system" (§15) true by construction instead of by code review vigilance.
- **Spacing scale:** a small fixed set of named steps (e.g., `xs`, `sm`, `md`, `lg`, `xl`) rather than arbitrary dp values chosen per screen.
- **Shape scale:** a small fixed set of named shapes (chip, card, sheet, button) rather than per-component radius literals — see §10.6 for the literal dp values behind each named shape.
- Lint/CI should treat a raw `Color(0x...)`, a raw `.dp` literal outside the token file, or a raw `RoundedCornerShape` outside the token file as a review flag.

### 10.2 Centralized Ingredient Icon System
- **One mapping service** (`IngredientIconService` or equivalent) is the only place in the app that decides what icon represents an ingredient. No composable looks up or hardcodes an icon for a specific ingredient name itself.
- Icons are **Material Symbols only** — there is no emoji anywhere in the ingredient system (chips, Pantry rows, recipe ingredient lists). This replaces the earlier implementation's per-ingredient emoji, which read as inconsistent and informal against the rest of the design system.
- Mapping is **category-based**, not one icon per canonical ingredient: ingredients resolve to a small set of categories (e.g., produce, protein, dairy, grain/starch, condiment/sauce, spice, other) and each category has one representative icon. This keeps the mapping maintainable as the ingredient catalog grows, rather than requiring a bespoke icon decision per new ingredient.
- **Generic fallback:** any ingredient that doesn't resolve to a known category renders a neutral, generic ingredient icon — the app never shows a broken/missing icon state.
- This service is called from every surface that renders an ingredient: `IngredientChip`, Pantry rows, and the "Your ingredients" / "Pantry staples" lists on Recipe Detail — one call site pattern, reused everywhere.

### 10.3 Recipe Hero Gradients
- Every recipe card and Recipe Detail screen shows a **gradient**, not a photograph, as its visual header.
- There are exactly **7 deterministic gradients** in the design system's fixed palette. The full hex/RGB/mood values for all 7 are given in §10.4.4 below.
- Selection is **stable and hash-based**, not random. The hash input is the recipe's `heroGradientSeed` (§19.6, §21.1) — in practice the recipe's ID, or its name only as a fallback before an ID exists — **never the display name on every read.** Hashing the name directly would mean a later name edit or re-localization changes a recipe's gradient, which breaks the "same recipe always renders the same gradient, on any device, on any viewing" guarantee this section requires. The gradient hash is `hash(heroGradientSeed) % 7`.
- **Gradient angle:** `110 + (hash(heroGradientSeed) % 60)` degrees, varying between 110° and 170° for visual variety across recipes while staying visually "on-brand."
- **Android implementation — resolved (v4.3):** Compose's `Brush.linearGradient` takes explicit `start`/`end` `Offset` values rather than a CSS-style angle. Convert with:
  ```kotlin
  fun angleToOffsets(angleDegrees: Float, width: Float, height: Float): Pair<Offset, Offset> {
      val angleRad = Math.toRadians(angleDegrees.toDouble())
      val cx = width / 2f
      val cy = height / 2f
      // Half-diagonal ensures the gradient line fully spans the box at any angle in [110, 170]
      val r = (kotlin.math.hypot(width.toDouble(), height.toDouble()) / 2).toFloat()
      val dx = (kotlin.math.cos(angleRad) * r).toFloat()
      val dy = (kotlin.math.sin(angleRad) * r).toFloat()
      return Offset(cx - dx, cy - dy) to Offset(cx + dx, cy + dy)
  }
  ```
  `HeroGradientBlock` (§12) computes `start`/`end` from its own measured `Size` using this helper — this is now the specified implementation, not an open decision.
- **Rationale (why this replaced hero photography):** it removes AI image-generation cost and latency, removes a CDN/image-loading dependency and its failure modes, and gives the app a distinctive, consistent visual identity that a stock-photo or AI-image approach could not guarantee. This is a direct expression of the product's "performance over decoration" principle (§15).
- The shared-element transition between a `RecipeCard` and Recipe Detail (§14 motion) still applies — the gradient block itself morphs between the two screens, preserving the "premium feel" the transition was originally designed to deliver, without needing a photograph to do it.

### 10.4 Color, Typography & Shape — Authoritative Token Values

This is the concrete data behind §10.1's rule that "every color, type style, spacing, and corner radius resolves through one theme source."

#### 10.4.1 Brand Foundation
- **Seed color:** `#E8622C` — `oklch(0.696 0.176 41)`. A warm, appetite-associated burnt orange, deliberately chosen to differentiate from SuperCook's green-eco branding and Samsung Food's blue-corporate branding (§4).
- This is the seed Material You's dynamic-color tonal-palette generation starts from on devices that support it. On devices without dynamic color support, the full static palette in §10.4.3–§10.4.4 is used directly.
- `#E8622C` is also the value bound to the Android status bar / system chrome color in light mode via `enableEdgeToEdge`/`Window.statusBarColor` (§10.7).

#### 10.4.2 Typography
| Tier | Font | Usage |
|---|---|---|
| Functional UI | **Geist** (variable sans) | All UI chrome, body text, buttons, labels |
| Editorial Display | **Fraunces** (variable serif, opsz + SOFT axes) | Recipe dish names, screen titles, headings |
| Monospace | **Geist Mono** | Tabular numbers, code/debug surfaces |

**Android implementation — resolved (v4.3):** Geist, Fraunces, and Geist Mono are bundled as variable font resources (`res/font/`), exposed through the Compose `Typography` object as `FontFamily`s, with Fraunces's `opsz`/`SOFT` axes applied via `Font(..., variationSettings = FontVariation.Settings(...))`. Given the locked minSdk of 26 (§16.11), this is supported across the app's full device range, so **no static-weight fallback is planned.** This is now a QA gate rather than an open design question: the release checklist (§26) includes a real-device check of Fraunces rendering at the minimum supported OS version; if that check surfaces a rendering gap on a specific OEM skin, the fix is scoped to that finding, not a speculative fallback built in advance.

#### 10.4.3 Color Tokens — Light Mode

**Surface**
| Token | OKLCH | Hex | RGB | Usage |
|---|---|---|---|---|
| `background` | `oklch(0.992 0.004 60)` | `#FCFAF7` | `252, 250, 247` | App background — warm off-white |
| `foreground` | `oklch(0.21 0.02 50)` | `#332B26` | `51, 43, 38` | Primary text — warm dark brown |
| `card` | `oklch(1 0 0)` | `#FFFFFF` | `255, 255, 255` | Card surfaces — pure white |
| `card-foreground` | `oklch(0.21 0.02 50)` | `#332B26` | `51, 43, 38` | Text on cards |
| `popover` | `oklch(1 0 0)` | `#FFFFFF` | `255, 255, 255` | Dropdown/popover background |
| `popover-foreground` | `oklch(0.21 0.02 50)` | `#332B26` | `51, 43, 38` | Text on popovers |

**Brand / Primary**
| Token | OKLCH | Hex | RGB | Usage |
|---|---|---|---|---|
| `primary` | `oklch(0.696 0.176 41)` | `#E8622C` | `232, 98, 44` | Primary brand color — CTAs, active nav, links, focus rings |
| `primary-foreground` | `oklch(0.99 0.01 80)` | `#FFFCF5` | `255, 252, 245` | Text on primary |

**Secondary / Neutral**
| Token | OKLCH | Hex | RGB | Usage |
|---|---|---|---|---|
| `secondary` | `oklch(0.96 0.018 70)` | `#F5EDE2` | `245, 237, 226` | Warm sand secondary surface |
| `secondary-foreground` | `oklch(0.32 0.03 55)` | `#5C4B3F` | `92, 75, 63` | Text on secondary |
| `muted` | `oklch(0.965 0.012 70)` | `#F7F1E8` | `247, 241, 232` | Muted background — skeleton base |
| `muted-foreground` | `oklch(0.5 0.025 55)` | `#8A7468` | `138, 116, 104` | Labels, timestamps, helper text |
| `accent` | `oklch(0.95 0.03 60)` | `#F3E8D9` | `243, 232, 217` | Hover/highlight surface |
| `accent-foreground` | `oklch(0.3 0.04 50)` | `#4D3D33` | `77, 61, 51` | Text on accent |

**Tertiary / Specialty**
| Token | OKLCH | Hex | RGB | Usage |
|---|---|---|---|---|
| `tertiary` | `oklch(0.82 0.13 75)` | `#E8C766` | `232, 199, 102` | Amber/gold — tips, ratings (§10.5) |
| `tertiary-foreground` | `oklch(0.28 0.05 60)` | `#4A3D1A` | `74, 61, 26` | Text on tertiary |
| `staple` | `oklch(0.9 0.04 145)` | `#D9E8D0` | `217, 232, 208` | Sage green — "Pantry staples used" card background (§11.4) |
| `staple-foreground` | `oklch(0.34 0.05 150)` | `#2D5A3D` | `45, 90, 61` | Text on staple |

**Semantic — Success**
| Token | OKLCH | Hex | RGB | Usage |
|---|---|---|---|---|
| `success` | `oklch(0.62 0.15 150)` | `#3D9B5C` | `61, 155, 92` | Completed/success states (§10.5) |
| `success-foreground` | `oklch(0.99 0.01 150)` | `#F0FFF5` | `240, 255, 245` | Text on success |
| `success-container` | `oklch(0.94 0.04 150)` | `#DCF0E1` | `220, 240, 225` | Success container background |
| `success-container-foreground` | `oklch(0.34 0.06 150)` | `#2D5A3D` | `45, 90, 61` | Text in success container |

**Semantic — Warning**
| Token | OKLCH | Hex | RGB | Usage |
|---|---|---|---|---|
| `warning` | `oklch(0.74 0.16 70)` | `#D99B3E` | `217, 155, 62` | Warning states (§10.5) |
| `warning-foreground` | `oklch(0.2 0.03 70)` | `#332814` | `51, 40, 20` | Text on warning |
| `warning-container` | `oklch(0.94 0.05 75)` | `#F5E8CC` | `245, 232, 204` | Warning container background |
| `warning-container-foreground` | `oklch(0.36 0.07 60)` | `#5C4818` | `92, 72, 24` | Text in warning container |

**Semantic — Destructive**
| Token | OKLCH | Hex | RGB | Usage |
|---|---|---|---|---|
| `destructive` | `oklch(0.577 0.22 27)` | `#C8362B` | `200, 54, 43` | Delete, error, sign-out |
| `destructive-foreground` | `oklch(0.99 0.01 27)` | `#FFF5F3` | `255, 245, 243` | Text on destructive |

**Border / Input / Ring**
| Token | OKLCH | Hex | RGB | Usage |
|---|---|---|---|---|
| `border` | `oklch(0.91 0.012 60)` | `#E8E0D5` | `232, 224, 213` | Card borders, dividers |
| `input` | `oklch(0.91 0.012 60)` | `#E8E0D5` | `232, 224, 213` | Text input borders |
| `ring` | `oklch(0.696 0.176 41)` | `#E8622C` | `232, 98, 44` | Focus ring |

**Chart Colors** *(reference only — no charts/nutrition UI in current scope; §21.1, §25)*
| Token | OKLCH | Hex | RGB |
|---|---|---|---|
| `chart-1` | `oklch(0.696 0.176 41)` | `#E8622C` | `232, 98, 44` |
| `chart-2` | `oklch(0.74 0.16 70)` | `#D99B3E` | `217, 155, 62` |
| `chart-3` | `oklch(0.62 0.15 150)` | `#3D9B5C` | `61, 155, 92` |
| `chart-4` | `oklch(0.55 0.16 290)` | `#6B4FA0` | `107, 79, 160` |
| `chart-5` | `oklch(0.7 0.13 200)` | `#4A9BB5` | `74, 155, 181` |

*(A `sidebar-*` token subset also exists in the original reference. It's a web layout concept — ScrapChef's Android nav is a bottom bar, §9.2 — so it's omitted; see §10.7.)*

#### 10.4.4 Color Tokens — Dark Mode

**Surface**
| Token | OKLCH | Hex | RGB |
|---|---|---|---|
| `background` | `oklch(0.17 0.012 50)` | `#2A2522` | `42, 37, 34` |
| `foreground` | `oklch(0.97 0.008 70)` | `#F5F1EA` | `245, 241, 234` |
| `card` | `oklch(0.225 0.014 50)` | `#3A332E` | `58, 51, 46` |
| `card-foreground` | `oklch(0.97 0.008 70)` | `#F5F1EA` | `245, 241, 234` |
| `popover` | `oklch(0.225 0.014 50)` | `#3A332E` | `58, 51, 46` |
| `popover-foreground` | `oklch(0.97 0.008 70)` | `#F5F1EA` | `245, 241, 234` |

**Brand / Primary**
| Token | OKLCH | Hex | RGB |
|---|---|---|---|
| `primary` | `oklch(0.80 0.15 45)` | `#F0915C` | `240, 145, 92` |
| `primary-foreground` | `oklch(0.20 0.03 45)` | `#332218` | `51, 34, 24` |

**Secondary / Neutral**
| Token | OKLCH | Hex | RGB |
|---|---|---|---|
| `secondary` | `oklch(0.29 0.02 55)` | `#4A413B` | `74, 65, 59` |
| `secondary-foreground` | `oklch(0.96 0.01 70)` | `#F0EAE0` | `240, 234, 224` |
| `muted` | `oklch(0.27 0.014 55)` | `#443D37` | `68, 61, 55` |
| `muted-foreground` | `oklch(0.72 0.02 65)` | `#B0A499` | `176, 164, 153` |
| `accent` | `oklch(0.34 0.04 50)` | `#564940` | `86, 73, 64` |
| `accent-foreground` | `oklch(0.96 0.012 70)` | `#F0EAE0` | `240, 234, 224` |

**Tertiary / Specialty**
| Token | OKLCH | Hex | RGB |
|---|---|---|---|
| `tertiary` | `oklch(0.78 0.12 75)` | `#D9B850` | `217, 184, 80` |
| `tertiary-foreground` | `oklch(0.2 0.03 60)` | `#332A14` | `51, 42, 20` |
| `staple` | `oklch(0.32 0.04 150)` | `#2A4D35` | `42, 77, 53` |
| `staple-foreground` | `oklch(0.88 0.04 150)` | `#C0DBC8` | `192, 219, 200` |

**Semantic — Success**
| Token | OKLCH | Hex | RGB |
|---|---|---|---|
| `success` | `oklch(0.7 0.14 152)` | `#52A86A` | `82, 168, 106` |
| `success-foreground` | `oklch(0.16 0.03 152)` | `#1A3322` | `26, 51, 34` |
| `success-container` | `oklch(0.3 0.05 152)` | `#244D2E` | `36, 77, 46` |
| `success-container-foreground` | `oklch(0.88 0.05 152)` | `#C0DBC8` | `192, 219, 200` |

**Semantic — Warning**
| Token | OKLCH | Hex | RGB |
|---|---|---|---|
| `warning` | `oklch(0.8 0.15 72)` | `#E0AC50` | `224, 172, 80` |
| `warning-foreground` | `oklch(0.2 0.03 72)` | `#332814` | `51, 40, 20` |
| `warning-container` | `oklch(0.32 0.06 65)` | `#4D3D1A` | `77, 61, 26` |
| `warning-container-foreground` | `oklch(0.9 0.05 70)` | `#E5D9B8` | `229, 217, 184` |

**Semantic — Destructive**
| Token | OKLCH | Hex | RGB |
|---|---|---|---|
| `destructive` | `oklch(0.66 0.19 25)` | `#D45040` | `212, 80, 64` |
| `destructive-foreground` | `oklch(0.98 0.01 25)` | `#FFF0EE` | `255, 240, 238` |

**Border / Input / Ring**
| Token | Value | Hex/Alpha | Usage |
|---|---|---|---|
| `border` | `oklch(1 0 0 / 10%)` | `#FFFFFF` at 10% alpha | Subtle translucent border |
| `input` | `oklch(1 0 0 / 14%)` | `#FFFFFF` at 14% alpha | Input border |
| `ring` | `oklch(0.78 0.15 45)` | `#F0915C` | Focus ring — matches primary |

Compose `Color` supports alpha directly (`Color.White.copy(alpha = 0.10f)`) — no conversion caveat needed here.

**Chart Colors** *(reference only, see §10.4.3 caveat)*
| Token | OKLCH | Hex | RGB |
|---|---|---|---|
| `chart-1` | `oklch(0.78 0.15 45)` | `#F0915C` | `240, 145, 92` |
| `chart-2` | `oklch(0.8 0.15 72)` | `#E0AC50` | `224, 172, 80` |
| `chart-3` | `oklch(0.7 0.14 152)` | `#52A86A` | `82, 168, 106` |
| `chart-4` | `oklch(0.68 0.15 290)` | `#8A6BB5` | `138, 107, 181` |
| `chart-5` | `oklch(0.74 0.12 200)` | `#5BAACA` | `91, 170, 202` |

#### 10.4.5 Light vs. Dark — Quick Comparison

| Token | Light | Dark | Strategy |
|---|---|---|---|
| `background` | `#FCFAF7` | `#2A2522` | Deep warm dark, not pure black |
| `foreground` | `#332B26` | `#F5F1EA` | Warm off-white, not pure white |
| `card` | `#FFFFFF` | `#3A332E` | Slightly lighter than bg for elevation |
| `primary` | `#E8622C` | `#F0915C` | Brightened for dark-background contrast |
| `secondary` | `#F5EDE2` | `#4A413B` | Warm sand → warm dark |
| `muted` | `#F7F1E8` | `#443D37` | Inverted |
| `muted-foreground` | `#8A7468` | `#B0A499` | Lightened for dark-mode visibility |
| `border` | `#E8E0D5` | 10% white | Translucent in dark mode |
| `input` | `#E8E0D5` | 14% white | Translucent in dark mode |
| `success` | `#3D9B5C` | `#52A86A` | Lightened for visibility |
| `warning` | `#D99B3E` | `#E0AC50` | Lightened |
| `destructive` | `#C8362B` | `#D45040` | Lightened |
| `tertiary` | `#E8C766` | `#D9B850` | Slightly darkened to prevent glare |
| `staple` | `#D9E8D0` | `#2A4D35` | Light sage → dark sage |

#### 10.4.6 Corner Radii (Shape Scale)

| Named shape (§10.1) | Value | Compose mapping |
|---|---|---|
| `radius-sm` | `8px` | `RoundedCornerShape(8.dp)` — chips, small badges, small buttons |
| `radius-md` | `10px` | `RoundedCornerShape(10.dp)` — inputs, medium elements |
| `radius-lg` (base `radius`) | `12px` | `RoundedCornerShape(12.dp)` — cards, list items |
| `radius-xl` | `16px` | `RoundedCornerShape(16.dp)` — large containers |
| `radius-chip` | `8px` | Ingredient chips specifically (matches `radius-sm`) |
| `radius-card` | `12px` | Recipe cards specifically (matches `radius-lg`) |
| `radius-sheet` | `20px` | Bottom sheets, primary CTA buttons |

These map directly onto `ScrapChefTheme.shapes` (§10.1) as `Shapes(small = 8.dp, medium = 10–12.dp, large = 16.dp)` plus the two named one-offs (`chip`, `sheet`) the theme object should expose explicitly rather than reusing `small`/`large` implicitly.

### 10.5 Semantic Color Roles — Usage Guidance (Locked)

| Role | Confirmed current-scope usage |
|---|---|
| `success` | Cook Mode completion state (§11.6); a generation-succeeded indicator; a "back online" banner recovering from §11.10's offline state |
| `warning` | The Recipe Error state's "no recipe possible with constraints" variant only (§11.3.1) — a soft, actionable mismatch, not a hard failure |
| `destructive` | The Recipe Error state's "invalid AI output" and "network/timeout" variants (§11.3.1); Delete Pantry item / Favorite; sign-out |
| `tertiary` (amber/gold) | The tip card on Recipe Detail (§11.4); the 5-star rating prompt on Cook Mode completion (§11.6) |
| `staple` (sage green) | The "Pantry staples used" section on Recipe Detail (§11.4) |

**Two usage examples from the original token reference do not apply to current scope and are not built against:**
- `warning` — "expiring soon alerts": pantry item expiry tracking is out of scope; `pantry_items` has no `expiry_date` column in this version (§21.1). Phase-2 note only (§25).
- `tertiary`/`warning` — "calorie badges" and "low-confidence chips": nutrition data is out of scope (§21.1, §25), and ingredient-recognition "confidence" scoring is a Vision-AI concept, explicitly deprecated (§27).

### 10.6 Motion & Animation Tokens (Locked)

| Token | Duration | Easing | Repeat | Maps to |
|---|---|---|---|---|
| shimmer | 1.6s | ease-in-out | infinite | `LoadingSkeleton` (§12) |
| heart-burst | 0.45s | `cubic-bezier(0.34, 1.56, 0.64, 1)` (overshoot) | once | Favorite toggle "delight" flourish (§14) |
| particle | 0.5s | ease-out | once, forwards | Heart particle burst accompanying heart-burst |
| timer-pulse | 1.4s | ease-out | infinite | Cook Mode timer completion (§11.6, §14) |
| fade-up | **250ms** | ease-out | once, forwards | `StreamingRecipeCard` section entrance (§11.3, §14) |

**Resolved in v4.3:**
- **`fade-up` duration locked to 250ms.** §14's original description and the merged token reference's keyframe disagreed (250ms vs. 400ms). 250ms is the value that ships — it matches the "streaming feels instant" UX principle (§15) better than a slower 400ms fade would (ADR-009, §28).
- **`badge-bounce` removed from the design system entirely**, not merely left unassigned. It had no confirmed target element, and its "infinite" repeat directly conflicts with §14's own rule that the favorite-toggle heart-burst is "the one budgeted delight flourish in the app... everything else stays calm and purposeful." An infinite-loop bounce on the difficulty badge (its only plausible candidate) would violate that principle, not extend it. See §27, ADR-011.
- **`wave` (voice input waveform) remains removed, dead code — delete on sight, do not port to Android.** Leftover from the deprecated voice-input feature (§27); this was already resolved in v4.2 and is restated here only for completeness of the motion-token table's history.

All motion respects the system-level reduced-motion accessibility setting, gating whichever Compose `AnimationSpec`s are built from this table.

### 10.7 Supplementary Web-Reference Details (Not Applicable to the Android Client)

The original token reference included a few details that are meaningful only in a CSS/web context. Listed once, for completeness, and explicitly out of scope for this Android app:

- Custom `::-webkit-scrollbar` styling (track/thumb color, opacity, radius, width) — Android scroll indicators are a system-level concern, not themed per-app in the same way.
- Card hover elevation defined as a CSS `box-shadow`/`transform` transition — the Android equivalent is Compose `Modifier.shadow`/tonal elevation with a press (not hover) state change, same visual intent, different mechanism.
- The `<meta name="theme-color">` / Next.js `viewport.themeColor` export — the Android equivalent is setting the status bar color via `enableEdgeToEdge`/`Window.statusBarColor` to `primary` (§10.4.1), not a literal meta tag.
- The `sidebar-*` token subset (§10.4.3) — a web layout concept with no Android equivalent, since navigation here is a bottom bar (§9.2).

---

## 11. Screen-by-Screen UI Specification

### 11.1 Home Screen
- `TopAppBar`: ScrapChef wordmark; settings icon top-right.
- **Hero input card:** "What's in your kitchen?" with a single primary button, **Add Ingredients** — no mode chips, no "Type / Speak / Snap" choice (§27). One tap goes straight to Ingredient Input.
- **Quick Suggestions:** a horizontally scrollable row of preset combinations (e.g., "15-minute dinners," "Surprise me") that skip straight to Generating with pre-set filters.
- **Recent Recipe(s):** the last few generated recipes as compact cards for instant re-open, using their stored hero gradient (§10.3).
- Footer microcopy: "We only ever use what you tell us you have, plus basic staples like salt, oil, and water."
- Bottom nav: Home tab active.

### 11.2 Ingredient Input Screen
- `TopAppBar`: back arrow (returns to Home, selections cleared), title "What do you have?"
- **One flow, no mode switch.** `IngredientTextField` — debounced (300ms) autocomplete search with synonym matching — is the only entry mechanism. Selected items become removable `IngredientChip`s in a `FlowRow`, each rendered with its icon from the Centralized Ingredient Icon System (§10.2).
- If the user has Pantry items, a small "From your Pantry" suggestion row appears above the text field for one-tap add.
- Below the chips: **Tools filter** (`FilterChip` row: Stovetop, Oven, Microwave, Air Fryer, No-Cook; multi-select; Stovetop default; at least one always selected) and **Time filter** (`SingleChoiceSegmentedButtonRow`: 15 / 30 / 45 / No limit; default 30).
- Primary button "Cook something" — full width, disabled until 3–8 ingredients and ≥1 tool are selected.
- Footer: "Pantry staples we always assume: salt, pepper, oil, water" (region-aware, §7).
- **Validation:** 3 ≤ ingredient count ≤ 8; at least one tool; time ∈ {15, 30, 45, 0} — unchanged from the prior spec, since none of this was voice/photo-specific.

### 11.3 Generating Screen
- Full-screen, no bottom nav. Content streams in progressively — dish name first, then time/tools, then "Your ingredients," then "Pantry staples," then steps one at a time, then the tip — rendered inline as the recipe assembles (§14 motion), not behind a separate loading overlay (§27 — the old `RecipeGeneratingOverlay` concept is gone; streaming renders directly into the eventual Recipe Detail layout).
- A small "Cancel" text button is available; confirmed only if generation is already well underway, to avoid losing an almost-finished recipe.
- On completion, the screen **is** Recipe Detail — no separate navigation jump.
- On guardrail failure after one retry (§19.6), the screen transitions to the Recipe Error state (§11.3.1) instead.

#### 11.3.1 Recipe Error State
- Friendly illustration + message, branching by cause: invalid AI output ("We couldn't quite get this one right. Want to try again?"), no recipe possible with the given constraints ("No recipe fits those ingredients and filters yet. Try adding one more ingredient or loosening a filter."), network/timeout ("Connection trouble. Check your signal and try again.").
- Two buttons: **Try Again** (re-sends the identical request) and **Adjust Ingredients** (back to Ingredient Input, prior selections intact).
- Color treatment (locked, §10.5): `destructive` for "invalid AI output" and "network/timeout"; `warning` for "no recipe possible with constraints."

### 11.4 Recipe Detail Screen
- `TopAppBar`: back arrow (preserves prior screen state), title "Recipe," **one** heart/favorite icon top-right — never a second favorite control elsewhere on the screen (§27 explicitly removes a prior duplicate).
- Hero gradient block (§10.3) at the top, dish name overlaid.
- Row: total time, tool icon(s), difficulty badge. **No "AI Recipe" badge** — every recipe in this product is AI-generated, so labeling it as such on every screen added visual noise without conveying new information (§27).
- "Your ingredients" (icons from §10.2) → "Pantry staples used" (visually distinct via the `staple` token, §10.4.3/§10.5, explicitly labeled as assumed) → "Steps" (numbered) → optional tip in an outlined card using the `tertiary` token (§10.5).
- Serving-size stepper scales ingredient quantities client-side from the structured recipe schema (§19.6) — never re-calls the AI just to scale servings.
- Primary bottom action: **Start Cooking** → Cook Mode (§11.6).

### 11.5 Recipe Detail — Offline Variant
- If already fully loaded, the recipe stays fully viewable offline; a slim, dismissible banner explains that new actions need a connection. No behavior here is voice/photo-related, so this is unchanged from the prior spec in substance.

### 11.6 Cook Mode
- Full-screen, screen-wake-lock enabled, system back gesture intercepted by an "Exit Cook Mode?" confirmation.
- One step at a time, large high-contrast type, advanced by **tap or swipe** only (§27).
- Any step with a timer shows a large circular timer control, **started by tap**, using the `timer-pulse` motion token (§10.6) on completion. Multiple timers can run concurrently and persist as a small chip stack pinned to the top of the screen regardless of which step is currently shown — this multi-timer behavior is unchanged and was never voice-dependent.
- Progress indicator (step X of Y) always visible.
- Completion state: a short congratulatory screen with a 5-star rating prompt (`tertiary` token, §10.5), then a button back to Home.

### 11.7 Pantry Screen
- List of ingredients the user has added, each row rendered with its icon (§10.2). FAB "+" → Pantry Add, which reuses the same `IngredientTextField` autocomplete component as Ingredient Input — one input component, used everywhere ingredients are entered, with no camera/barcode entry path (§27).
- A "Recipes from your Pantry" button jumps straight into Generating using Pantry contents as the ingredient set, pre-selected up to 8, user-adjustable before generating.

### 11.8 Favorites Screen
- Grid/list of saved recipes, each card showing its hero gradient (§10.3), dish name, time, and the single favorite/heart toggle to unfavorite. Empty state: friendly icon, one sentence, "Cook something" CTA back to Home.

### 11.9 Settings Screen
- Sectioned list: **Country & Language**, **Dietary Preferences**, **Units** (Metric/Imperial override), **Privacy & Data**, **Help & Feedback**.
- This is the **only** place these values are ever set. There is no per-request localization/dietary field anywhere else in the app (§18.3) — this is intentional and central to why the product feels like it has "nothing to configure twice."

### 11.10 Offline / No-Connectivity State
- Unchanged in substance from the prior spec: a full-width banner blocks new generation with a "Retry" affordance when there's no connection; already-loaded Recipe Detail, Pantry, and Favorites remain browsable.

---

## 12. Component Library

| Component | Purpose |
|---|---|
| `IngredientTextField` | The single autocomplete input, reused in Ingredient Input and Pantry Add |
| `IngredientChip` | Removable selected-ingredient chip, rendered with an icon from `IngredientIconService` (§10.2) |
| `ToolFilterChip` | Icon + label selectable chip for cooking tools |
| `TimeSegmentedButton` | Single-choice time selector |
| `StreamingRecipeCard` | Progressive-reveal recipe assembly component driving §11.3, using the `fade-up` motion token (§10.6) |
| `RecipeCard` (compact) | Grid/list card for Favorites, Recents, quick suggestions — renders a hero gradient, never a photo |
| `HeroGradientBlock` | Resolves a recipe's deterministic gradient and computes its `Brush.linearGradient` offsets (§10.3), shared between `RecipeCard` and Recipe Detail for the shared-element transition |
| `TimerChip` | Active countdown chip, stackable, used in Cook Mode |
| `ErrorIllustration` | Standardized empty/error visual + message + action button(s) |
| `LoadingSkeleton` | Shimmer placeholder matching target content shape, using the `shimmer` motion token (§10.6) |

**Removed relative to the prior document:** `VoiceInputButton`, `PhotoCaptureCard`, and any component that took an `inputMode` parameter. See §27 for the full removal list.

---

## 13. Interaction Performance Targets

These are **acceptance-testing targets on a defined reference device/network profile** (§16.11), not universal guarantees — actual numbers vary by device class, network conditions, and current AI model latency. They were never voice/photo-specific and remain the product's performance bar:

| Interaction | Target |
|---|---|
| Cold app start to interactive Home | < 1.5s on a mid-range device |
| Tap → screen transition | < 100ms perceived |
| Ingredient autocomplete keystroke → dropdown update | < 300ms (debounce window) |
| First streamed token of AI generation | < 1.5s from tap |
| Full recipe generation (streaming) | < 10s p95 end-to-end |
| Scrolling | Sustained 60fps minimum, zero dropped-frame jank on Recipe Detail and Cook Mode |

## 14. Motion & Microinteractions
- **Shared-element transition:** a `RecipeCard`'s `HeroGradientBlock` morphs into the Recipe Detail's hero block on tap — still the single most important "premium feel" interaction in the app, now built entirely from vector gradients rather than photography, which if anything makes it cheaper and more reliable to implement well.
- **Streaming content:** each new section of `StreamingRecipeCard` fades up (8dp translate + fade, **250ms**, §10.6) as it arrives.
- **Chip selection:** scale-bounce (1.0 → 1.05 → 1.0, ~150ms) + color fill.
- **Timer completion (Cook Mode):** pulse + haptic + audible chime, perceivable from across the kitchen. (§10.6 `timer-pulse`.)
- **Favorite toggle:** the one budgeted "delight" flourish in the app (heart fill + small burst, §10.6 `heart-burst`/`particle`) — everything else stays calm and purposeful, per the "no unnecessary UI" principle (§15). This is also why `badge-bounce` was removed rather than assigned to an element (§10.6, §27).

---

## 15. UX Principles

These replace the prior document's "Fast / Multimodal / AI Companion" framing, which no longer describes this product:

1. **One recipe over many options.** Generation always produces a single confident hero result, never a list to sort through.
2. **Minimal taps.** Every flow is measured in taps, and every added tap needs a specific justification.
3. **Streaming feels instant.** The AI response streams into a layout that's already visible and ready — the user watches a recipe assemble, never watches a spinner.
4. **No unnecessary UI.** If a screen element doesn't directly serve entering ingredients, reading a recipe, or cooking it, it doesn't belong.
5. **Everything follows the design system.** No hardcoded colors, typography, spacing, radii, or one-off icons anywhere (§10).
6. **Performance over decoration.** When a design choice trades speed or reliability for visual richness (e.g., AI hero photography), the leaner, faster choice wins — the hero-gradient system (§10.3) is the clearest example of this principle in practice.

---

## 16. Engineering Standards & Conventions

Concrete, checkable rules that make §2's principles enforceable rather than aspirational.

### 16.1 Package Structure
```
app/

core/
    designsystem/      — theme, typography, tokens (§10.1, §10.4)
    common/             — shared utilities, Result wrappers (§16.6)
    network/            — API client, SSE streaming
    datastore/          — Room + DataStore persistence

feature/
    home/
    ingredients/
    recipe/
    pantry/
    settings/
    favorites/

domain/                 — use cases, domain models
data/                   — repositories, data sources
ai/                     — prompt building, OpenRouter client, guardrails (server-side equivalents live in the Recipe Service, §18.1)
navigation/             — nav graph (§9.1)
```
Every new file belongs in exactly one of these; a feature module never reaches into another feature module's internals, only into `core`/`domain`.

### 16.2 Dependency Policy (Expanded, v4.3)
| | |
|---|---|
| **Allowed** | Jetpack Compose, Material 3, Kotlin Coroutines, Ktor, Room, Coil, Navigation Compose, Lifecycle Compose (`ViewModel`, `lifecycle-runtime-compose`), `kotlinx.serialization`, DataStore Preferences, OpenRouter (server-side) |
| **Forbidden** | Reflection-based libraries, the legacy Android View system, XML layouts, a second networking library alongside Ktor, a second serialization library alongside `kotlinx.serialization`, **a dependency-injection framework (Hilt, Koin, or otherwise) — see below** |
A new dependency outside the allowed list requires an explicit `DECISIONS.md` entry (§29) before it's added, not after.

**Dependency injection — resolved, not deferred (ADR-012, §28).** ScrapChef uses **manual dependency injection**: a small composition root (per §16.1's module boundaries) constructs and wires dependencies via plain constructor injection, with `CompositionLocal` used only where Compose idiom requires it (e.g., providing the composition root itself to the Compose tree). No DI framework is added. This follows directly from Engineering Principle #1 (simplicity over cleverness) and #9 (every dependency needs justification): a project this size doesn't need a framework's compile-time code generation and build-time cost to solve a wiring problem a plain factory/composition root already solves. If the dependency graph grows enough that manual wiring becomes genuinely painful, that's a real signal worth revisiting — record that decision in `DECISIONS.md` at the time, rather than defaulting to a framework now on the assumption it might be needed later (§2, principle 5).

### 16.3 Coding Standards
| Limit | Value |
|---|---|
| Max file length | 400 lines |
| Max composable function | 120 lines |
| Max function | 40 lines |
| Max nesting depth | 3 |
| Max cyclomatic complexity | <10 |
A file, composable, or function that exceeds these should be split, not exempted.

### 16.4 Definition of "Clean Code" (enforced at review, not aspirational)
No dead code. No `TODO`s left behind. No commented-out code. No duplicated code. No magic numbers. No hardcoded strings (use localized string resources). No hardcoded colors, typography, spacing, or radii (§10.1, §10.4). No hardcoded configuration values (§16.9). No unused imports. No unused resources. No deprecated APIs.

### 16.5 Performance Budget
These are **acceptance targets verified against the reference device/network profile (§16.11)**, not per-device guarantees — real-world numbers vary with device class, network conditions, and AI model latency (see §13's framing, which applies here too).

| Budget | Target |
|---|---|
| APK size | < 30 MB |
| RAM (average) | < 180 MB |
| Cold start | < 1.5 s (§13) |
| Jank | < 1% of frames |
| Database query | < 100 ms |
| Prompt Builder (§18.3) | < 10 ms |
These are measured, not estimated — CI performance tests (§24) should fail the build on a regression past these numbers.

### 16.6 Error Handling Rules
- Every repository function returns `Result<T>`, never throws across a layer boundary.
- Every network/AI error is mapped to a domain error type before it reaches a ViewModel — a ViewModel never inspects a raw HTTP status code or SSE error string directly.
- Every UI state is explicit (§17.1's `GenerationState` sealed interface is the reference pattern) — no implicit "null means loading" conventions.

### 16.7 AI Coding Rules
Rules specifically for an AI coding agent working in this codebase:
- Never rewrite working code without a specific reason tied to a bug, a requirement in this document, or a principle in §2.
- Never rename a public API without updating every reference.
- Never change the architecture described in §17–§21 without recording why in `DECISIONS.md`.
- Never introduce a duplicate component or service where one already exists — search before creating (§16.1's structure makes this searchable by construction).
- Prefer extending existing code over parallel implementations.
- Delete obsolete code immediately when it's replaced — never leave two implementations of the same thing side by side "just in case." This includes dead design-token artifacts inherited from a merged reference document (§10.6's `wave` keyframe), not just code originating in this codebase.
- Follow the naming conventions in §16.10 for every new class, function, and file — an AI agent should not need to infer naming style from nearby examples when it's specified here directly.

### 16.8 Refactoring Policy
Refactor only when it measurably improves performance, readability, maintainability, or consistency with this document. Do not refactor for stylistic preference alone — a diff with no behavioral or measurable quality change is not worth the review cost or the risk of regression.

### 16.9 Configuration Policy (New, v4.3)
Never hardcode, anywhere in the codebase:
- Base URLs (the `/v1` API host, §20)
- API keys or secrets (the OpenRouter key never ships client-side at all, §18.1, §22)
- Feature flags
- Timeout values (network, AI generation, retry backoff)
- Model names/slugs (§19.1)

All of the above come from build-variant configuration (`BuildConfig` fields generated per environment, §24's staging/production separation) on the client, and from environment variables / secrets management on the backend (§22). This product's current scope has no runtime remote-config service (that would be new backend surface not defined in §18.1) — environment-specific `BuildConfig` values plus the existing staged-rollout process (§24) are sufficient for this version. If a genuine need for live, no-redeploy flag flipping emerges later, that's a new piece of architecture to propose via `DECISIONS.md`, not something to build speculatively now (§2, principle 5).

### 16.10 Package & Class Naming Conventions (New, v4.3)
| Layer | Suffix | Example |
|---|---|---|
| Repository interface (domain) | `Repository` | `RecipeRepository` |
| Repository implementation (data) | `RepositoryImpl` | `RecipeRepositoryImpl` |
| Use case (domain) | `UseCase`, named as a verb phrase | `GenerateRecipeUseCase`, `AddPantryItemUseCase` |
| Network response model | `Dto` | `RecipeDto` |
| Persistence model (Room) | `Entity` | `RecipeEntity`, `PantryItemEntity` |
| Presentation-layer model | `UiModel` | `RecipeUiModel` |
| DTO/Entity ↔ domain/UI conversion | `Mapper` | `RecipeMapper` |
| Remote data source | `RemoteDataSource` | `RecipeRemoteDataSource` |
| Local data source | `LocalDataSource` | `PantryLocalDataSource` |
| ViewModel | `ViewModel` | `RecipeDetailViewModel` |
| Screen-level composable | `Screen` | `RecipeDetailScreen` |
| Reusable component (§12) | descriptive noun, no suffix | `IngredientChip`, `HeroGradientBlock` |
| Backend service (§18.1) | `Service` | `RecipeService`, `PantryService` |

A `Mapper` only ever converts between two adjacent layers (`Dto` ↔ domain model, or domain model ↔ `UiModel`) — it never spans three layers at once (§16.7's "no duplicate business logic" applies to mapping logic too).

### 16.11 Version Matrix (New, v4.3 — Locked)

| Component | Version | Notes |
|---|---|---|
| Min SDK | API 26 (Android 8.0) | Product decision, balancing modern API surface (adaptive icons, notification channels, reliable variable-font rendering, §10.4.2) against device reach |
| Target SDK / Compile SDK | API 36 (Android 16) | Required for all new Google Play submissions and updates as of the August 31, 2026 deadline |
| Kotlin | 2.4.0 | Latest stable at time of writing; confirm compatibility with the AGP version in use before bumping, since AGP 9.x's built-in Kotlin support (below) is still an actively evolving mechanism |
| Android Gradle Plugin (AGP) | 9.2.x | Requires JDK 17; stabilizes Kotlin 2.x compatibility |
| Gradle | 9.5.x | Matches AGP 9.2.x's minimum Gradle requirement |
| Jetpack Compose BOM | `androidx.compose:compose-bom:2026.06.00` | Pin via BOM per §16.2; don't pin individual Compose artifact versions separately |
| JDK | 17 | Required by AGP 9.x |

**Governance, not open-endedness:** these values are locked as of August 2026. They are revisited on a defined cadence — at each annual Google Play target-API deadline, and whenever the toolchain is deliberately upgraded — not through silent drift. Any deliberate change to this table gets a `DECISIONS.md` entry recording the "why" and the date, per §16.7. The one genuinely externally-driven exception is the Kotlin/AGP pairing note above: AGP 9.0+'s built-in Kotlin support is new enough that its interaction with an independently-chosen Kotlin version is worth a quick confirmation at whatever moment implementation actually begins or the toolchain is next bumped.

---

## 17. State Management & Data Flow (Android)

### 17.1 Architecture Pattern
Clean Architecture + MVVM, unidirectional data flow, Jetpack Compose observing `StateFlow`.

```kotlin
data class ScrapChefUiState(
    val selectedIngredients: List<Ingredient> = emptyList(),
    val ingredientQuery: String = "",
    val selectedTools: Set<CookingTool> = setOf(CookingTool.STOVETOP),
    val selectedMaxTimeMinutes: Int = 30, // 0 = no limit
    val generationState: GenerationState = GenerationState.Idle,
    val recipe: Recipe? = null,
    val error: ScrapChefError? = null,
    val isOffline: Boolean = false
)

sealed interface GenerationState {
    data object Idle : GenerationState
    data class Streaming(val partialRecipe: PartialRecipe) : GenerationState
    data class Success(val recipe: Recipe) : GenerationState
    data class Failed(val error: ScrapChefError) : GenerationState
}
```
No `inputMode` field, no `IngredientInputMode` enum (§27). Country, language, dietary constraints, and measurement system are **not** part of this per-request state at all — they live only in the user's Settings-backed profile and are attached server-side by the AI Prompt Pipeline (§18.3), not carried through the client's request-building state.

**State ownership (New, v4.3).** MVVM defines the pattern; this defines who owns what, so an AI agent never has to guess:

```
UI (Composable)
   │  observes StateFlow, sends UI events
   ▼
ViewModel
   │  invokes
   ▼
UseCase (domain)
   │  invokes
   ▼
Repository (domain-facing interface; §17.3 for its contract)
   │  coordinates
   ▼
Remote data source (Ktor/API, §18) + Local data source (Room/DataStore, §17.2)
```

Ownership flows one direction only:
- A Composable never holds a reference to another screen's `ViewModel`.
- A `ViewModel` never constructs or calls a `Repository` directly — only a `UseCase`.
- **UI never accesses a Repository directly**, even for something that looks trivial, like a simple Pantry read. Skipping the `UseCase` layer "just this once" is exactly the kind of architectural drift §16.7's AI coding rules exist to prevent — if a `UseCase` feels like pure pass-through for a given read, that's fine; the layer still exists so the boundary stays consistent everywhere.

### 17.2 Persistence
- Room holds: Pantry items, Favorites, a short-TTL session cache of recently viewed recipes (performance cache, not an offline store), and a local mirror of Settings values for instant UI hydration.
- Settings values (country, language, dietary preferences, units) are account-linked and backend-authoritative; Room is a cache, server wins on conflict — unchanged from the prior architecture's online-first stance.

### 17.3 Repository Contract (New, v4.3)
- A `Repository`'s public API returns **domain models only** — never a `Dto` or an `Entity` (§16.10) crossing into `domain`/`feature` code. Mapping happens inside the `data` layer (§16.1) via a `Mapper`.
- A `Repository` coordinates local and remote data sources (cache-then-network, server-wins-on-conflict per §17.2) — that coordination logic lives in the `Repository`, not duplicated in a `UseCase` or `ViewModel`.
- A `Repository` contains no UI logic, no display formatting, no localization decisions — those belong to a `UiModel`/`Mapper` closer to the `ViewModel` (§16.10).
- A `Repository`'s functions return `Result<T>` (§16.6) — never throw across the `data` → `domain` boundary.

---

## 18. Backend Architecture

### 18.1 Service Topology
Leaner than the prior document — **no Vision Service** (§27):

| Service | Responsibility |
|---|---|
| **API Gateway** | Auth verification, rate limiting, request routing/logging |
| **Recipe Service** | Prompt construction, the Settings → AI Prompt Pipeline (§18.3), OpenRouter call, streaming relay, guardrail validation (§19.6), caching |
| **Ingredient Service** | Canonical ingredient catalog, synonym/localized-name resolution, autocomplete search index, category mapping backing the Ingredient Icon System (§10.2) |
| **Pantry Service** | Persistent pantry CRUD |
| **User Service** | Auth, profile, Settings values (country, language, dietary preferences, units) |

There is no image upload path in this product (§27).

### 18.2 Infrastructure
Unchanged in shape from the prior document (containerized, autoscaled services; managed PostgreSQL; managed Redis for the AI response cache and rate limiting; CDN — now only needed for app assets/icons, not for hero photography, since §10.3 removed that dependency entirely). See §21 for the trimmed database schema.

### 18.3 Settings → AI Prompt Pipeline
This is the architectural pattern that replaces per-request localization/dietary fields from the prior spec.

```
User Settings (Country, Language, Dietary Preferences, Units)
        │
        ▼
   Profile Store (User Service)
        │  fetched once per request, server-side, by user ID —
        │  never sent by the client as part of the generation request body
        ▼
   Prompt Builder (Recipe Service)
        │  merges: {user ingredients, tools, time} from the client
        │        + {country, language, dietary preferences, units} from Settings
        ▼
      OpenRouter  →  Streaming SSE  →  Guardrails  →  Recipe
```
- The client's `POST /recipes/generate` request body carries **only** what the user chose in that flow — ingredients, tools, max time (§19.5). It does not carry country, language, dietary constraints, or units.
- The Recipe Service looks up the authenticated user's Settings server-side and injects them into the prompt automatically. This guarantees Settings changes apply to every future request immediately, with no risk of a stale value being cached client-side in a request payload, and keeps the client-side request shape simple.
- If a user has no Settings configured yet (should not happen post-onboarding, but defensively handled), the Recipe Service falls back to device-locale-derived defaults rather than failing the request.

---

## 19. AI Architecture

### 19.1 Model Layer: OpenRouter
All model calls are made server-side, from the Recipe Service only — never from the client, and never with a provider API key shipped in the app (unchanged security posture from the prior document).

```
User Input → Prompt Builder → OpenRouter → Streaming SSE → Guardrails → Recipe
```

- ScrapChef calls a single unified endpoint (OpenRouter's OpenAI-compatible `chat/completions` API) rather than integrating directly against a specific provider's SDK (§27, ADR-001 in §28).
- **Model selection:** a specific fast, cost-effective model slug is configured server-side (not hardcoded in this document, since available slugs and pricing on OpenRouter change frequently — confirm the current best fast/affordable text model at implementation time and record the choice in `DECISIONS.md`). **This remains the one deliberately open configuration item in this document** — unlike the items resolved in v4.3, it depends entirely on a third party's current offering, not on this team's own design judgment.
- **Fallback:** OpenRouter supports specifying a fallback model list in the request so that a provider outage or rate limit doesn't fail the user's request outright; the Recipe Service should configure at least one fallback model in a similar cost/speed tier to the primary.
- **Structured output:** recipe responses are requested as constrained JSON (`response_format: json_schema` where the selected model supports it, `json_object` plus strict server-side schema validation otherwise) against the schema in §19.6 — support for strict JSON-schema-constrained output varies by underlying model on OpenRouter, so the guardrail/validation pipeline (§19.6) must never assume the model-level constraint alone is sufficient.
- **Streaming:** requested via `stream: true`; the Recipe Service re-emits typed SSE events to the client per semantic unit (name → tools/time → ingredients → each step → tip), not raw token deltas — this is what powers `StreamingRecipeCard` (§11.3, §14) and is unchanged in design from the prior document, only the upstream provider changed.
- There is no multimodal call anywhere in this architecture. The model is only ever asked to transform structured text input into structured text/JSON output.

### 19.2 Retrieval Grounding
Unchanged in principle from the prior document: a small vector-indexed set of technique-verified reference snippets (standard ratios, cook times/temperatures) is retrieved by ingredient/cuisine similarity and injected into the prompt to keep technique accurate, without loosening the strict ingredient-containment rule in §19.6. This remains a reasonable investment even in the leaner MVP, since it directly improves recipe quality at low cost.

### 19.3 AI Guardrails (Anti-Hallucination — Unchanged Pipeline, Simplified Scope)
Every AI response passes through this pipeline before reaching the client. Removing photo/voice input did not remove the need for this — if anything, with OpenRouter able to route to many different underlying models, strict server-side validation matters more, since output reliability now varies by whichever model is currently configured.

1. **Strict schema validation** against §19.6 — unknown or missing fields fail.
2. **Ingredient containment check** — every ingredient-like noun in `steps`/`name`/`tip` must resolve to the user's provided ingredients, the region-appropriate pantry staples, or a small fixed vocabulary of non-ingredient cooking terms.
3. **Tool check** — `tools` in the response must be a subset of the user's selected tools.
4. **Time check** — `totalTimeMinutes` ≤ the user's max time filter (when not "no limit").
5. **Dietary constraint check** — every ingredient checked against the user's Settings-configured dietary tags (injected via §18.3), not left to model judgment alone.
6. **Safety check** — reject steps with unsafe food-handling patterns (e.g., raw high-risk protein without an accompanying cook/heat instruction).
7. **Retry-with-correction** — on first failure, retry once with an amended prompt naming the violated constraint. A second failure surfaces the Recipe Error state (§11.3.1) — the app never serves a response that failed guardrails.
8. Every guardrail failure is logged with its violation type for prompt-quality monitoring (§23) — a rising failure rate is a signal to revise the prompt (§19.4), and is worth watching more closely now that the underlying model can be swapped via OpenRouter's routing. Per the logging policy (§22), the violation *type* is logged, never the offending prompt/response content verbatim.

### 19.4 Prompt Library

**System prompt — recipe generation:**
```
You are ScrapChef, a precise, zero-waste home-cooking assistant. Generate exactly one
recipe using ONLY the ingredients the user provided and the allowed pantry staples for
their region: {regional_staples_list}. Do NOT invent, substitute, or add any other
ingredient. Use only the cooking tools specified: {user_tools}. The total time
(prep + cook) must not exceed {max_time} minutes (0 means no limit). Respect these
dietary constraints without exception: {dietary_tags}. Respond entirely in {language}.
Bias cuisine style toward {country}-appropriate cooking where it fits the given
ingredients, without forcing a mismatch. Use the following verified technique reference
notes where relevant: {retrieved_grounding_snippets}. Output ONLY valid JSON conforming
to the provided schema — no extra commentary, no markdown fences.
```

**User message template:**
```
Ingredients: {ingredient_list}
Tools: {tool_list}
Max total time: {max_time} minutes (0 = no limit)
```
Note what's no longer in the user message: `Country`, `Language`, `Dietary constraints`, and `Units` used to be listed here as explicit fields in the earlier document. They are now supplied by the Prompt Builder from Settings (§18.3), not assembled from client-submitted request fields — the system prompt above is where they're injected.

**Guardrail retry prompt (appended on first validation failure):**
```
Your previous response violated the following constraint(s): {violated_constraints}.
Regenerate the recipe using ONLY the exact ingredients and staples listed above. Do not
introduce any ingredient, tool, or time value outside what was specified.
```

**No vision/image-recognition prompt exists in this version** (§27).

### 19.5 Ingredient Normalization & Autocomplete
Unchanged from the prior document, since this was never voice/photo-related: canonical ingredient catalog with per-language display names and synonyms; `ILIKE`/trigram-indexed search; 300ms debounce; 20-result cap.

### 19.6 JSON Schema — Recipe Response
```json
{
  "name": "string, 1-80 chars",
  "totalTimeMinutes": "integer > 0",
  "difficulty": "enum: easy | medium | hard",
  "tools": ["enum: stovetop | oven | microwave | airfryer | no-cook"],
  "yourIngredients": [
    {"name": "string", "amount": "number", "unitMetric": "string", "unitImperial": "string"}
  ],
  "pantryAssumptions": ["string, subset of the allowed regional staples list"],
  "steps": [
    {"index": "integer", "text": "string", "timerSeconds": "integer | null"}
  ],
  "tip": "string | null",
  "heroGradientSeed": "string — the value hashed to select the deterministic gradient (§10.3); typically the eventual recipe ID, assigned client- or service-side, not authored by the model"
}
```
Removed relative to the prior schema: `heroImagePrompt` and the entire `nutrition` object (nutrition tracking was part of the broader Phase 2 vision and is not part of this leaner version's scope — see §25 if it's reintroduced later). No fields outside this schema are permitted in the model's structured-output configuration.

---

## 20. API Specification

Base URL: `https://api.scrapchef.app/v1` (placeholder — environment-configured per §16.9, never hardcoded in client code).

### 20.1 `POST /recipes/generate` (streaming, SSE)
**Auth:** required.
**Request:**
```json
{
  "ingredients": ["egg", "rice", "soy sauce", "green onion"],
  "tools": ["stovetop"],
  "maxTimeMinutes": 30
}
```
Note the request body contains **only** ingredients, tools, and time — no `country`, `language`, `dietaryConstraints`, or `measurementSystem` fields (§18.3, §19.4). These are resolved server-side from the authenticated user's Settings.

**Validation:** `ingredients` 3–8 items, each 1–50 chars; `tools` non-empty, values in the fixed enum; `maxTimeMinutes` ∈ {15, 30, 45, 0}.
**Response:** SSE stream, terminating in `recipe_done` or `recipe_error`.
**Errors:** `400 VALIDATION_ERROR`, `422 NO_RECIPE_POSSIBLE`, `429 RATE_LIMITED`, `500 AI_RESPONSE_INVALID`, `504 AI_TIMEOUT`.

### 20.2 `GET /ingredients/search?q={query}&lang={language}`
Unchanged from the prior document — public, IP-rate-limited, capped at 20 results.

### 20.3 Pantry, Favorites, and Settings Endpoints
Unchanged in shape from the prior document's `§18.6–18.9`, minus anything vision/barcode-related:
- `GET /pantry`, `POST /pantry/items`, `PATCH /pantry/items/{id}`, `DELETE /pantry/items/{id}`
- `GET /favorites`, `POST /favorites`, `DELETE /favorites/{recipeId}`
- `GET /me`, `PATCH /me` (country, language, dietary constraints, measurement system — this is the only place these fields are ever written)

**Removed entirely:** `POST /vision/recognize-ingredients` and any barcode-lookup endpoint (§27).

### 20.4 API Versioning Policy (New, v4.3)
- All endpoints are versioned under `/v1` (§20's base URL already reflects this).
- A **breaking change** — removing or renaming a field, changing a field's type or semantics, removing an endpoint — requires a new version prefix (`/v2`). It is never introduced in place under `/v1`.
- **Additive, backward-compatible changes** (a new optional field, a new endpoint) may ship under the existing `/v1` without a version bump.
- `/v1` is supported for a minimum of **12 months** after `/v2` ships, with deprecation communicated via a response header on `/v1` calls once `/v2` exists.
- The exact deprecation-header mechanics depend on whatever managed API gateway is chosen (§18.2) and its native support for standard deprecation/sunset headers — this is the one detail here still worth a `DECISIONS.md` entry, since it's genuinely gateway-product-dependent rather than a design choice this document can make unilaterally.

---

## 21. Database Design

### 21.1 Relational Schema (PostgreSQL — system of record)

**`users`**
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| email | text unique | nullable if OAuth-only |
| country | char(2) | ISO 3166-1 alpha-2 |
| language | text | BCP-47 |
| measurement_system | text | metric \| imperial |
| created_at | timestamptz | |

**`dietary_preferences`** — `(user_id FK, tag text)` composite PK.

**`pantry_items`** — `(id PK, user_id FK, canonical_ingredient_key, quantity numeric null, unit text null, added_at)`.

**`favorites`** — `(id PK, user_id FK, recipe_id FK, favorited_at)`.

**`recipes`** — `(id PK, owner_user_id FK null, name, payload jsonb [the §19.6 schema], country, language, created_at, cache_key text indexed)`.

**`ingredients`** — `(canonical_key PK, category text)` — `category` is the field the Ingredient Icon System (§10.2) maps to an icon.

**`ingredient_localizations`** — `(canonical_key FK, language text, display_name text, synonyms text[])` composite PK.

**`regional_pantry_staples`** — `(country char(2), staple_canonical_key FK)` composite PK.

**Removed relative to the prior schema:** any `expiry_date` column on `pantry_items` (expiry tracking was Phase 2 scope, not part of this version — see the §10.5 caveat about the `warning` token's "expiring soon" usage example), and any image/photo-upload-related table.

### 21.2 Vector Database
Unchanged from the prior document: one collection (`technique_reference`) for the grounding retrieval described in §19.2.

### 21.3 Cache Layer (Redis)
`recipe_cache:{cache_key}` (30-day TTL) and `ratelimit:{user_id}:{date}` — unchanged in purpose from the prior document. Cache key is a hash of the normalized ingredient/tools/time request **plus** the user's country/language/dietary values (since those now come from Settings rather than the request body, they must still be folded into the cache key server-side, or two users with identical ingredients but different Settings would incorrectly share a cached recipe).

---

## 22. Auth, Security & Privacy

- **Auth:** Google Sign-In and email/password via a managed auth provider; short-lived JWT access token + refresh flow. Unchanged from the prior document.
- **No guest mode** — Settings (country/language/dietary) and Pantry/Favorites are account-scoped, so an account is required before first generation.
- **No admin/subscription tier logic in this version** — the prior document's `free`/`pro` split and paywall screens are part of the broader monetization vision (§25) and are not part of this leaner MVP's scope; do not build a paywall against this document.
- **Security posture, unchanged:** TLS everywhere; the OpenRouter API key lives only in backend secrets management, never in client code; parameterized queries only; R8/ProGuard in release builds; rate limiting at the API Gateway.
- **Privacy, simplified:** since there is no photo upload path in this version, the prior document's photo-retention policy (§22.1 in the old spec) no longer applies — there is nothing to retain or delete on that front. Data collected is limited to account identity, Settings values, Pantry contents, and Favorites/recipe history. GDPR/CCPA data export and full account deletion remain required, first-class Settings surfaces, not "contact support" flows.
- **Logging policy (New, v4.3):** never log, in any environment — AI prompts sent to or responses received from the model; API keys or other secrets; JWTs/refresh tokens; email addresses; or raw user-generated recipe content, ingredient lists, or dietary tags tied to a user ID. The prompt-quality monitoring described in §19.3 logs guardrail violation *types* only, never the offending prompt/response content verbatim, and never joined to an identifiable user in the log record.
- **Configuration policy:** see §16.9 for the full "never hardcode" list (URLs, API keys, feature flags, timeouts, model names) — restated as a pointer here because it's a security-relevant rule, not just a code-cleanliness one.

---

## 23. Performance, Caching & Accessibility

- **Client cache:** Room-backed session cache of recently viewed recipes (performance only, not offline storage — unchanged principle from the prior document, §0).
- **Backend AI cache:** per §21.3 — 30-day TTL, replayed as a simulated stream on a cache hit so the progressive-reveal experience is identical whether the response is fresh or cached.
- **No image CDN dependency for hero content** (§10.3, §27) — the CDN is only needed for static app assets and icons.
- **Accessibility:** WCAG 2.1 AA equivalents throughout — 48dp minimum touch targets, full semantics/`contentDescription` coverage defined at the component level (§12), TalkBack support on every screen, dynamic text scaling to 200% without truncation. Cook Mode's large-type baseline remains an accessibility-motivated default. Motion (§10.6, §14) respects the system reduced-motion setting.

---

## 24. Analytics, Testing & CI/CD

- **Analytics events (anonymized, no PII, no advertising ID):** `onboarding_completed`, `generate_recipe_requested` (params: ingredient_count, tool_count), `generate_recipe_succeeded`/`_failed` (params: cache_hit, latency_ms, failure_reason), `recipe_viewed`, `cook_mode_started`/`_completed`, `favorite_added`/`_removed`, `pantry_item_added`, `error_occurred`. Removed relative to the prior document: any event with an `input_mode` parameter, since there's only one input mode now.
- **Testing:** unit tests (ViewModels, guardrail logic, validators) via JUnit5/MockK; integration tests (Room in-memory, API contract tests against §20) via WireMock/MockWebServer; Compose UI tests for every screen in §11; an AI/prompt golden-set regression harness that re-runs a curated set of ingredient/tool/time combinations against the prompt library (§19.4) on every prompt change and checks guardrail pass rates — this matters more now that OpenRouter can route to different underlying models over time. 100% of guardrail checks (§19.3) require explicit unit tests. Design-token conformance (§10.4) should also be covered by a lint rule/test that fails CI on a raw hex/`.dp`/`RoundedCornerShape` literal outside the token file (§10.1, §16.4).
- **CI/CD:** lint, unit tests, Compose UI tests, and the AI golden-set harness on every PR; signed release builds via staged Play Console rollout (internal → closed alpha → open beta → production); staging/production environment separation for API base URLs and the OpenRouter key (§16.9).

---

## 25. Business Model & Future Phases

This version of ScrapChef does not include monetization, meal planning, nutrition tracking, community features, or grocery/appliance integrations — all of that remains plausible future-phase work from the earlier, broader blueprint, but none of it should be built against this document. If and when the product expands again, that work should get its own addendum to this spec (not a silent re-expansion of scope), so the "this document = current implementation" guarantee in §0 stays true. This explicitly includes pantry expiry tracking and nutrition-badge/confidence-scoring UI referenced only as usage examples in the merged design tokens (§10.5) — the tokens exist, the features don't yet.

---

## 26. Acceptance Criteria & Definition of Done

- **AC01:** A user can enter 3–8 ingredients through the single autocomplete flow (§11.2) — no mode switch exists or is needed.
- **AC02:** Tool and time filters function exactly as specified, including the "can't deselect the last tool" rule.
- **AC03:** Recipe generation streams progressively (§11.3), first content visible in <1.5s, full completion in <10s p95.
- **AC04:** Every generated recipe passes 100% of the guardrail checks in §19.3 before reaching the client.
- **AC05:** Country, language, and dietary preferences measurably change generated recipe content, and are sourced **only** from Settings (§18.3) — never re-entered per request.
- **AC06:** Every ingredient rendered anywhere in the app (chip, Pantry row, recipe list) uses an icon resolved through the Centralized Ingredient Icon System (§10.2) — zero emoji anywhere in the ingredient system.
- **AC07:** No screen, composable, or style value uses a hardcoded color, typography style, spacing value, or corner radius outside the design token source (§10.1, §10.4).
- **AC08:** Every recipe's hero visual is one of exactly 7 gradients, deterministically selected by a stable hash of the recipe's `heroGradientSeed` (§10.3, §19.6) — the same recipe always renders the same gradient.
- **AC09:** Recipe Detail shows exactly one favorite control (§11.4) — never two.
- **AC10:** No "AI Recipe" badge or label appears anywhere in the app.
- **AC11:** Cook Mode is fully navigable by tap/swipe alone, with no microphone permission requested and no voice command handling anywhere in its implementation (§11.6).
- **AC12:** No screen, service, dependency, or API endpoint related to voice input, camera/photo capture, barcode scanning, or Vision AI exists anywhere in the codebase (§27) — including no leftover dead-code animation assets for those features (§10.6).
- **AC13:** All AI model calls route through OpenRouter's unified API — there is no direct integration against a single provider's SDK.
- **AC14:** The app never crashes on invalid AI output, malformed API responses, or network loss — all such cases resolve to a defined error state (§11.3.1, §11.10).
- **AC15:** All screens meet the accessibility bar in §23, verified with TalkBack and 200% font scale.
- **AC16 (New, v4.3):** No repository function returns a `Dto` or `Entity` across the `data`/`domain` boundary, and no Composable calls a `Repository` directly (§17.1, §17.3).
- **AC17 (New, v4.3):** Nothing in the codebase logs a raw AI prompt/response, API key, JWT, email address, or user-tied recipe content (§22).

### Release Checklist (Pre-Ship Gate) — New, v4.3
Every release candidate clears all of the following before rollout begins (§24's staged rollout):
- [ ] Lint passes with zero new suppressions
- [ ] Unit, integration, and Compose UI tests pass (§24)
- [ ] AI/prompt golden-set harness pass rate at or above the established baseline (§19.3, §24)
- [ ] Accessibility pass: TalkBack walkthrough + 200% font scale on every screen (§23, AC15)
- [ ] Localization spot-check on at least one RTL language (Arabic or Urdu) and one CJK language, in addition to English (§7)
- [ ] Fraunces variable-font rendering spot-checked on a device at the minimum supported OS version (§10.4.2)
- [ ] Performance targets verified against the reference device/network profile (§13, §16.5 — acceptance targets, not per-device guarantees)
- [ ] Crash-free smoke test on the staged rollout's internal track before promoting to closed alpha (§24)
- [ ] Release notes updated

---

## 27. Deprecated Concepts — Do Not Reintroduce

These features, screens, components, services, and class names existed in an earlier version of the product or its documentation and **must not be reintroduced, referenced, or pattern-matched against** when extending this codebase. If a future requirement seems to call for one of these, treat that as a signal to re-read §0–§5 and confirm the requirement is actually in scope before building anything — it likely represents scope creep back toward the abandoned broader vision, not a legitimate gap in this document.

**Removed features:**
- Voice ingredient input, voice commands, voice onboarding, always-listening microphone, on-device or cloud speech recognition of any kind.
- Photo/camera ingredient recognition, fridge/pantry scanning, barcode scanning, any multimodal AI capability.
- AI-generated or stock hero photography for recipes (replaced by deterministic gradients, §10.3).
- Emoji as ingredient icons (replaced by the Material-Symbols-only Ingredient Icon System, §10.2).
- The "AI Recipe" badge on Recipe Detail.
- A second/duplicate favorite control on any screen.
- Nutrition tracking, meal planning, subscription/paywall, and community features (these are future-phase concepts per §25, not deprecated-forever, but are **not** part of this document's scope and must not be built against it).

**Removed architecture/components:**
- `VoiceInputButton`, `PhotoCaptureCard`
- `InputChipMode` / any `inputMode` enum or parameter on navigation routes or UI state
- `GeminiRepository`, `GeminiApiService` (replaced by the OpenRouter-based Recipe Service integration, §19.1)
- `RecipeGeneratingOverlay` (replaced by inline `StreamingRecipeCard` rendering, §11.3)
- The Vision Service (§18.1) and any image-upload handling path
- The `wave` CSS keyframe (a "voice input waveform" animation) found in the merged design-token reference — dead code retained after voice input was cut. Delete on sight; do not port to Android (§10.6).
- **New in v4.3:** the `badge-bounce` motion token (§10.6). Not dead code exactly — it was simply never assigned to a confirmed UI element, and its infinite-repeat behavior conflicts with §14's "one budgeted delight flourish" rule. Removed from the design system rather than left as an open question (ADR-011, §28).
- **New in v4.3:** any dependency-injection framework (Hilt, Koin). Manual DI is the specified approach (§16.2, ADR-012).

**Removed navigation:**
- `ingredient_input/{mode}` — the correct route is `ingredient_input`, no parameter (§9.1).

---

## 28. Architecture Decision Records (ADR)

A running log of the decisions that shaped this version. Add new entries here as they're made — this is the mechanism that keeps future changes traceable instead of silently drifting from the document again (the failure mode §0 exists to prevent).

- **ADR-001 — OpenRouter selected** as the model-routing layer over a direct single-provider integration (§19.1).
- **ADR-002 — Hero gradients instead of images.** Seven deterministic, hash-selected gradients replace AI-generated or stock recipe photography (§10.3).
- **ADR-003 — No voice input.** Cut from the product entirely; see §27.
- **ADR-004 — No camera/photo ingredient recognition.** Cut from the product entirely; see §27.
- **ADR-005 — Single recipe generation only.** No meal planning or multi-recipe modes in this version (§4.2, §8).
- **ADR-006 — Server-side prompt pipeline.** Country, language, and dietary preferences are resolved from Settings server-side, never sent per-request from the client (§18.3).
- **ADR-007 — Design tokens merged into the spec.** Color, typography, radius, gradient, and motion values previously living in a separate CSS/OKLCH reference document are now defined once in §10.4–§10.6 as the single authoritative source; Android binds these as Compose `Color`/`Dp`/`FontFamily` values rather than consuming CSS at build time (Engineering Principle #3, §2).
- **ADR-008 — Hero gradient hash input confirmed as `heroGradientSeed`.** Not the recipe's display name, so a later name edit or re-localization never changes a recipe's gradient (§10.3, §19.6).
- **ADR-009 (New, v4.3) — `fade-up` streaming animation duration locked to 250ms**, resolving the conflict between §14's original description and the merged token reference's 400ms keyframe value (§10.6).
- **ADR-010 (New, v4.3) — Recipe Error state color mapping locked:** `destructive` for "invalid AI output" and "network/timeout"; `warning` for "no recipe possible with constraints" (§10.5, §11.3.1).
- **ADR-011 (New, v4.3) — `badge-bounce` removed from the design system.** No confirmed element justifies it, and it conflicts with the "one budgeted flourish" rule in §14 (§10.6, §27).
- **ADR-012 (New, v4.3) — No dependency-injection framework.** Dependencies are wired manually via constructor injection through a small composition root, per Engineering Principles #1 and #9 (§16.2).
- **ADR-013 (New, v4.3) — Repository contract formalized:** domain models only across the `data`/`domain` boundary, `Result<T>` returns, no UI logic in repositories (§17.3).
- **ADR-014 (New, v4.3) — API versioning policy adopted:** `/v1` now, breaking changes require `/v2`, minimum 12-month `/v1` support overlap after `/v2` ships (§20.4).
- **ADR-015 (New, v4.3) — Toolchain version matrix locked** as of August 2026 (Kotlin 2.4.0, AGP 9.2.x, Gradle 9.5.x, Compose BOM 2026.06.00, JDK 17, minSdk 26 / target-compileSdk 36) — §16.11. Superseded only by an explicit `DECISIONS.md` entry when the toolchain is deliberately upgraded, not by silent drift.
- **ADR-016 (New, v4.3) — Gradient angle → Compose `Offset` conversion resolved** with a stated formula (§10.3), removing the prior "log a conversion helper in DECISIONS.md" deferral.

---

## 29. Open Questions & Assumptions Log

This list is now deliberately short. As of v4.3, ambiguity is treated as a defect to close during spec review, not a default state to leave for implementation (§0.7). What remains here depends on something genuinely outside this team's control, not on a design decision this document could have made itself:

- **OpenRouter model slug and fallback chain** (§19.1) — available models and pricing on OpenRouter change frequently; this is a configuration decision made at implementation time and recorded in `DECISIONS.md`, not something this document can pin without going stale immediately.
- **API gateway deprecation/sunset header support** (§20.4) — depends on which managed API gateway product is chosen (§18.2) and its native header conventions; confirm and record once that choice is made.
- **Whether nutrition, meal planning, or monetization are ever reintroduced** (§25) — a genuine future product decision, not an implementation ambiguity. If it happens, it gets a dated addendum to this spec, not a silent re-expansion of scope.

---
**End of Document**
