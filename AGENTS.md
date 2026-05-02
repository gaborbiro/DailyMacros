# AGENTS.md

## Cursor Cloud specific instructions

This is a native Android app (Daily Macros) — there is no backend, no Docker, and no web server. Development tasks are build, test, and lint via Gradle.

### Git workflow (agents)

- **Before starting work** on an existing branch, run `git pull` (prefer `git pull --rebase origin <branch-name>` if the branch may have moved) so local work is based on the latest remote tip.
- **If work continues after a PR was merged or closed**, do not reuse that PR branch for new changes: check the PR state (e.g. `gh pr view <n> --json state`), then branch from current **`master`** with a new `cursor/...-8ebf` branch.

### Environment prerequisites

- **JDK 17** (`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`)
- **Android SDK** at `/opt/android-sdk` with platform `android-36` and `build-tools;36.0.0`
- `local.properties` in the repo root must contain `sdk.dir=/opt/android-sdk`

### Key commands

All commands require `JAVA_HOME` and `ANDROID_HOME` to be set (already in `~/.bashrc`).

| Task | Command |
|---|---|
| Build debug APK | `./gradlew assembleDebug` |
| Build release AAB | `./gradlew bundleRelease` |
| Run unit tests | `./gradlew testDebugUnitTest` |
| Lint | `./gradlew lintDebug` |
| Update version catalog | `./gradlew versionCatalogUpdate` |

### CI/CD

- **CI** (`.github/workflows/android.yml`): runs on push/PR to `master` — tests, `assembleDebug`; on push to `master` only, creates a GitHub Release with tag `build-<shortSha>` and the debug APK.
- **Release** (`.github/workflows/release.yml`): triggered by **push to `release`** (e.g. after merging a PR into that branch; protect `release` from direct pushes in GitHub). Builds signed release AAB, creates a GitHub Release with tag **`v` + `appVersionName`** from **`app/build.gradle.kts`** (via `:app:writeAppReleaseVersionNameFile`), publishes to Google Play internal testing track. Bump **`appVersionName`** / **`appVersionCode`** in **`app/build.gradle.kts`** before merging to `release`; reusing a tag will fail the release step.
- Release signing reads keystore from env vars (`RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`). In CI, the base64-encoded keystore is decoded from the `RELEASE_KEYSTORE_BASE64` secret.

### Code conventions

- **Domain modules (`*:domain`, Kotlin `domain` source sets)**: Hold **only** stable types the app agrees on — entities, value objects, repository interfaces, and similar **data-shaped** definitions. Do **not** put **any** executable behavior there: no business rules, no I/O, no parsing, no sorting/filtering for presentation, and **no mapping** (including DTO→domain, domain→UI strings, or “building lines of text”). Mapping and orchestration belong in **repository implementations**, **feature/use-case** layers, or **mappers** colocated with those layers — never in `*:domain`.
- **Room database version**: Do **not** bump `AppDatabase` **`version`** or add a new **`Migration(n, n+1)`** while a change set is still on an **unmerged** branch or PR. Fold all schema edits for that work into the **single** migration that will ship with the merge (e.g. extend the existing `9 → 10` step instead of adding `10 → 11`). After merge to `master`, the next feature may introduce a new version/migration as usual. (If you already ran an older draft migration locally, clear app data or uninstall once before retesting.)
- **Use cases**: Each use case class exposes **exactly one** entry point named **`execute`**. Do not add extra public methods on use cases or pack unrelated behavior into them; keep each use case focused on a single responsibility. UI-held state (e.g. flags for a form) belongs in the **ViewModel / UI state models**, not in a use case. **Do not** define use cases inside **`repositories/*`** modules — repositories expose data; use cases orchestrate one or more repositories (or other services) and therefore live in the **feature** that needs them (e.g. `app/.../features/<feature>/usecase/`), or in **`features/common`** when multiple features share the same use case. Prefer `features/*` over `:app` when adding new shared code; much legacy UI still lives in `:app` until a broader refactor moves it.
- **Product / architecture**: Prefer a **wider view of the app** over local patches. If the user’s request can be met more simply or more reliably elsewhere (e.g. another API call that already persists to the DB), **say so** and recommend the alternative even when it implies a larger or more invasive change.

### Notes

- **`CHATGPT_API_KEY`**: workflows expect a repository secret with that **exact** name under **Settings → Secrets and variables → Actions**. Secrets stored only under **Codespaces** are **not** available to GitHub Actions; duplicate the value there if CI builds need the key. Locally, `local.properties` can define the same key for builds.
- This is a purely client-side Android app; there is no emulator in the Cloud Agent VM, so "running the app" means building the APK. Manual UI testing requires an Android 12+ device/emulator.
- Debug signing keystore is committed at `signing/keystore.jks` (password: `keystore`).
- Release signing uses a separate production keystore, never committed to the repo.
