## Polish pass for ScrapChef — repo hygiene, code quality, docs, and i18n

Scope (per your selection): **Repo hygiene + docs**, **Code quality fixes**, **String extraction (i18n)**. Respecting `.agents/AGENTS.md`: no package/repo/`applicationId`/DB-name renames, no branding rewrite, incremental one-task-per-commit workflow, and I will **not** delete the empty `docs/{ROADMAP.md,CHANGELOG.md,ADR/}` (they're referenced as live conventions, not stale scaffolding).

Verification gate after each code-touching commit: `./gradlew assembleDebug` + `./gradlew testDebugUnitTest`. (Device run only if an emulator is available — I'll check, but build+unit-tests is the reliable gate here.)

---

### Phase 1 — Repo hygiene (1 commit)
Untrack committed junk and broaden `.gitignore`. No behavior change.

- `git rm --cached` (untrack, stop polluting history going forward):
  - `logcat_output.txt` (1.2 MB), `logs/startup-verification.log` (+ `logs/`), `window_dump.xml`
  - `analyze_logo.py`, `generate_icons.py` (dev tooling, not app source — keep local copies, just untrack)
- Leave `metadata.json` tracked (uncertain whether it's environment/plugin metadata; conservative per freeze rules)
- Broaden `.gitignore`: add `logcat*.txt`, `logs/`, `metadata.json` (optional), `/analyze_logo.py`, `/generate_icons.py`
- Delete local throwaway files from the working tree: all `window_dump*.xml` (~30), `logcat*.txt`, `log_all.txt`, `recipe_gen_logs.txt`
- Remove Android Studio scaffolding tests: `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`, `ExampleRobolectricTest.kt`, and `app/src/test/screenshots/greeting.png` (real tests `MainActivityTest.kt` / `RecipeViewModelTest.kt` / `MockOpenRouterApiService.kt` stay)
- Build + unit tests to confirm nothing breaks → commit

### Phase 2 — Code quality fixes (1 commit)
- `data/repository/SettingsRepository.kt`: route the 4 raw `Log.e(TAG, ...)` calls (L49/59/69/79) through the existing `ScrapChefLogger`
- `ui/screens/GeneratingScreen.kt:331`: replace redundant `displayRecipe.tips!!.isNotEmpty()` (after a null check) with `displayRecipe.tips?.isNotEmpty() == true`
- `data/remote/OpenRouterRepository.kt:153`: replace `firstError!!` with explicit null handling
- `app/build.gradle.kts`: remove the commented-out unused-dependencies block (L72+) per the inline note
- Build + unit tests → commit

### Phase 3 — Docs (1 commit)
- Add `LICENSE` (MIT, matching README's claim) — README currently references a file that doesn't exist
- Fix the `your-username` placeholder in the README clone URL (correctness fix only; branding/tagline untouched per freeze)
- Commit

### Phase 4 — String extraction / i18n (several commits, grouped to avoid a giant 50-file commit)
`strings.xml` currently holds only `app_name`; `stringResource` is used 0× across 17 UI files. I'll extract user-visible strings (including `contentDescription` for a11y) into `res/values/strings.xml`, organized with per-screen comments, and replace inline literals with `stringResource(R.string.…)` / `stringResource(R.string.…, arg)` for parameterized text. Dynamic DB/data strings stay as-is.

- **4a — Foundation + onboarding cluster:** add the strings.xml structure; extract `SplashScreen`, `LandingScreen`, `OnboardingScreen`, `CountryLanguageScreen`, `Animations.kt` (label only). Build + test → commit.
- **4b — Pantry/input cluster:** `PantryScreen`, `PantryAddScreen`, `IngredientInputScreen`, `IngredientAutocomplete`, `IngredientChip`, `PantryBottomBar`, `TimeSegmentedButtons`, `ToolFilterChips`. Build + test → commit.
- **4c — Recipe/favorites/profile cluster:** `GeneratingScreen`, `KitchenModeScreen`, `FavoritesScreen`, `DietaryPreferencesScreen`, `ProfileScreen`, `MainActivity`. Build + test → commit.
- **4d — ViewModel error strings (contained refactor):** `RecipeViewModel.kt` emits hardcoded English (L208/214 RecipeError messages; L317–337 error fallbacks). Since `stringResource` is composable-only, resolve these in the UI via the existing `RecipeErrorType` enum (keep the enum as the error contract; move the display text into a UI-side resolver). Build + test → commit.

Each 4x commit is independently buildable and regression-checkable. I'll keep every change mechanical and additive (no logic changes), and stop to report if anything unexpected surfaces.

### Notes / caveats
- I commit only locally (`git add` + `git commit`); I will **not** push unless you ask.
- Each commit gets a clear conventional-commit message (`chore:`, `refactor:`, `docs:`, `feat(i18n):`).
- If a gradle build or unit test fails after any step, I stop and report rather than forcing through.