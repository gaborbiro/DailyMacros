# DailyMacros – Claude Code Project Guide

## Project overview

Native Android app (Daily Macros). No backend, no Docker, no web server. Development tasks are build, test, and lint via Gradle.

## Build commands

| Task | Command |
|---|---|
| Build debug APK | `./gradlew assembleDebug` |
| Build release AAB | `./gradlew bundleRelease` |
| Run unit tests | `./gradlew testDebugUnitTest` |
| Lint | `./gradlew lintDebug` |
| Update version catalog | `./gradlew versionCatalogUpdate` |

## Testing

- For deep-link changes, consider `@DeeplinkTest` coverage.

## CI/CD

- **CI** (`.github/workflows/android.yml`): runs tests + `assembleDebug` on push/PR to `master`. On merge to `master`, creates a GitHub Release tagged `build-<shortSha>`. On PRs from the same repo, creates a prerelease tagged `pr-<PR#>-<shortSha>`.
- **Release** (`.github/workflows/release.yml`): triggered by push to `release` branch. Builds signed AAB, creates a Release tagged `v<appVersionName>`, publishes to Google Play internal track. **Bump `appVersionName` / `appVersionCode` in `app/build.gradle.kts` before merging to `release`; reusing a tag fails the release step.**
- Release signing uses env vars: `RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` (base64 keystore from `RELEASE_KEYSTORE_BASE64` secret in CI).
- Debug keystore is committed at `signing/keystore.jks` (password: `keystore`).
- `CHATGPT_API_KEY` must be a repository Actions secret (not only Codespaces). Define locally in `local.properties`.
- Do not introduce secrets into source control.
