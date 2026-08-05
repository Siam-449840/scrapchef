# ScrapChef Architectural Decisions (DECISIONS.md)

This document tracks technical deviations, architecture choices, and rationale during the Phase 1 MVP rewrite.

## 1. Complete Removal of Offline Static Recipes
- **Context**: The old `ScrapChef` codebase contained hardcoded static recipes (e.g. `SeedData.kt`) that acted as a fallback when offline.
- **Decision**: Eradicated all traces of `StaticRecipeEntity` and `SeedData.STATIC_RECIPES`. `RecipeViewModel` now immediately surfaces an `OFFLINE_NO_MATCH` error state if the network call fails.
- **Rationale**: Strict adherence to PRD §0.1 ("No hardcoded/static recipe database fallback").
- **Consequence**: `AppDatabase` schema was bumped to version 3, using `fallbackToDestructiveMigration` since `StaticRecipeEntity` was dropped.

## 2. Pantry Storage
- **Context**: PRD §8.3 required a real Pantry inventory view rather than the static `PantryGuideScreen`.
- **Decision**: Created `PantryItemEntity` and `PantryDao` within Room. Added a full CRUD flow via `PantryViewModel` and new UI screens (`PantryScreen` and `PantryAddScreen`).
- **Rationale**: Enables users to persist their ingredient inventory securely on-device.

## 3. Strict Adherence to UI Spec
- **Context**: PRD mandated removal of fake UI placeholders like 5-star ratings or non-functional "Save to PDF" buttons.
- **Decision**: Purged spec-violating UI from `RecipeCard` and `RecipeResultScreen`. Replaced them with actual required features (e.g. Serving Size stepper, Start Cooking FAB).

## 4. Kitchen Mode Layout
- **Context**: PRD §8.2.2 requires a horizontal paging layout for active cooking.
- **Decision**: Implemented `KitchenModeScreen` using `HorizontalPager` with integrated timers per step.
- **Rationale**: Meets MVP requirements for a distraction-free cooking experience without over-engineering voice commands in Phase 1.

## 5. Settings & Onboarding Flow
- **Context**: Need to track Dietary Preferences and Onboarding state.
- **Decision**: Implemented Jetpack `DataStore` to store `Set<String>` of dietary preferences and an `onboarding_completed` boolean. Added a simple Onboarding flow on first launch.
- **Rationale**: DataStore is the modern, async replacement for `SharedPreferences`, ideal for simple state persistence.
