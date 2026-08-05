# ScrapChef Project Rules

## 1. Architectural & Branding Freeze
- Never rename packages again unless explicitly requested.
- Never rename repositories unless explicitly requested.
- Never change `applicationId` again.
- Never change the database name again.
- Never rewrite README branding again unless asked.
- Avoid large-scale refactors unless they solve a concrete, proven problem. 
- Restructuring is DONE. Every commit should either: fix a bug, implement a feature from the specification/roadmap, or improve test coverage.

## 2. Immutable Specification
- The file `docs/ScrapChef_Engineering_Specification_v4.3.md` is **IMMUTABLE**.
- Never modify it.
- If requirements change in the future:
    - update `docs/ROADMAP.md`
    - update `docs/CHANGELOG.md`
    - create an ADR if architecture changes

## 3. Engineering Workflow
- Use Phase terminology: `Planning Phase -> Foundation Phase -> Implementation Phase -> Stabilization -> Release`.
- For execution, follow this loop exactly to prevent giant 50-file commits:
    1. Pick one task
    2. Implement
    3. Build
    4. Run on device
    5. Regression test affected flow
    6. Commit
    7. Repeat

## 4. Current Phase Directives
- **We are entering the Implementation Phase.**
- The immediate priority is NOT adding new features. The priority is making the MVP rock solid:
    1. Complete the full regression checklist.
    2. Fix every bug found.
    3. Ensure zero startup/runtime crashes.
    4. Verify recipe generation end-to-end.
    5. Verify persistence (Room/DataStore).
    6. Only then begin implementing new UX improvements from the roadmap.
