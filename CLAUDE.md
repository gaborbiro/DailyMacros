# DailyMacros – Claude Code Project Guide

## Project overview

Native Android app (Daily Macros). No backend, no Docker, no web server. Development tasks are build, test, and lint via Gradle on Windows with PowerShell (`.\gradlew.bat`).

## Build commands

| Task | Command |
|---|---|
| Build debug APK | `.\gradlew.bat assembleDebug` |
| Build release AAB | `.\gradlew.bat bundleRelease` |
| Run unit tests | `.\gradlew.bat testDebugUnitTest` |
| Lint | `.\gradlew.bat lintDebug` |
| Update version catalog | `.\gradlew.bat versionCatalogUpdate` |

## Architecture

### Module structure

Required data flow: UI (Compose) → ViewModel → domain/use-case → repository interface → service interface → API/SDK.

| Path | Purpose |
|---|---|
| `features/<name>/` | Feature modules; all new feature code goes here |
| `features/common/<name>` | Concerns shared by multiple features |
| `features/common` | Code shared by all features |
| `repositories/*/domain` | Repository interfaces exposed as domain types |
| `services/*/domain` | Service interfaces exposed as domain types |
| `core:design` | Visual tokens, Compose components, typography, theme |

### Guardrails

- No boundary leaks (e.g. UI directly calling service implementations).
- No new cross-module coupling without matching existing patterns.
- Keep UI state and side effects coordinated by ViewModel.
- Throw/propagate domain-level errors using the `DomainError` pattern.

### Domain modules (`*:domain`)

Hold **only** stable types: entities, value objects, repository interfaces. No executable behavior — no business rules, no I/O, no parsing, no sorting/filtering for presentation, and **no mapping** (DTO→domain, domain→UI strings, etc.). Mapping belongs in repository implementations, feature/use-case layers, or mappers colocated with those layers.

### Use cases

- Expose exactly **one** entry point: `execute()`. No extra public methods; one responsibility per class.
- UI-held state (form flags, etc.) belongs in ViewModel / UI state models — not in use cases.
- Do not define use cases inside `repositories/*` modules.
- Place use cases in `features/<feature>/usecase/` or in `features/common` for shared ones.

### Room database versioning

Do **not** bump `AppDatabase` `version` or add a new `Migration(n, n+1)` while the change is still on an unmerged branch. Fold all schema edits into the single migration that ships with the merge. After merge to `master`, the next feature may introduce a new version/migration.

## Feature delivery

When implementing a feature:
1. Inspect at least one similar existing feature and mirror its structure and naming.
2. Implement all layers: UI (Compose), ViewModel/state, domain/use-case, repository/service wiring.
3. Maintain unidirectional data flow with ViewModel-driven state.
4. Use Hilt DI patterns already present in the codebase.
5. Integrate analytics/navigation/remote-config only through existing core module APIs.
6. Run unit tests before finalizing.

Do not: invent new architectural layers, generic "utility" abstractions, or ad-hoc base classes. Do not place business logic in composables or leave TODO placeholders for required behavior.

Prefer a **wider view of the app** over local patches. If the user's request can be met more simply elsewhere (e.g. an existing API call that already persists to the DB), say so and recommend the alternative even if it implies a larger change.

## Kotlin conventions

- Prefer small, explicit types and names that match nearby feature code.
- Keep coroutine and Flow usage consistent with existing ViewModel/repository patterns.
- Handle errors explicitly; never swallow exceptions.

## Gradle and dependencies

- Use existing version catalog conventions in `gradle/libs.versions.toml`.
- Respect pinned/kept versions (`# @keep`, `# @pin`).
- Avoid introducing new dependencies when equivalent project modules already exist.
- Keep changes minimal and coherent; avoid opportunistic refactors unrelated to the request.

## Resources and design system

- String naming format: `[where].[section].[description]` (default section is `content`).
- Group strings by screen/section; avoid reuse except for shared dialogs.
- Build UI with existing Compose patterns.
- Reuse tokens/components from `core:design` for fonts, colors, typography, and theme.

## Copy standards

- Use typographic apostrophe `'` in user-facing text. Do **not** use escaped `\'`.
- Add all new user-facing strings to `values/strings.xml`.

## Testing

- Run `.\gradlew.bat testDebugUnitTest` for the unit test baseline.
- For Android behavior changes, run targeted instrumentation tests on the `test35apis_playstore` emulator AVD.
- For deep-link changes, consider `@DeeplinkTest` coverage.

### Failing test protocol

For each failing test:
1. Identify the protected behavior first.
2. Classify the failure: production regression, intentional behavior change, outdated expectation, broken setup/environment, or flakiness.
3. If the test still reflects intended behavior, fix production code — not the test.
4. Never delete/skip/weaken tests, loosen assertions, add arbitrary sleeps, or over-mock just to pass.
5. If coverage is narrowed, add equivalent/better coverage or explicitly document the gap.
6. After fixing, report: root cause, code changes, test changes, protected behavior, remaining risk.

## Git workflow

- Before starting work on an existing branch: `git pull --rebase origin <branch-name>`.
- If a PR was merged or closed, do not reuse that branch. Check with `gh pr view <n> --json state`, then branch from current `master`.
- Branch naming convention: `claude/...-<shortSha>`.

## CI/CD

- **CI** (`.github/workflows/android.yml`): runs tests + `assembleDebug` on push/PR to `master`. On merge to `master`, creates a GitHub Release tagged `build-<shortSha>`. On PRs from the same repo, creates a prerelease tagged `pr-<PR#>-<shortSha>`.
- **Release** (`.github/workflows/release.yml`): triggered by push to `release` branch. Builds signed AAB, creates a Release tagged `v<appVersionName>`, publishes to Google Play internal track. **Bump `appVersionName` / `appVersionCode` in `app/build.gradle.kts` before merging to `release`; reusing a tag fails the release step.**
- Release signing uses env vars: `RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` (base64 keystore from `RELEASE_KEYSTORE_BASE64` secret in CI).
- Debug keystore is committed at `signing/keystore.jks` (password: `keystore`).
- `CHATGPT_API_KEY` must be a repository Actions secret (not only Codespaces). Define locally in `local.properties`.
- Do not introduce secrets into source control.
