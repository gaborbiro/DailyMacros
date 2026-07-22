# DailyMacros – Claude Code Project Guide

## Project overview

Native Android app (Daily Macros). A small Firebase Cloud Functions backend (`functions/`) proxies OpenAI calls and enforces spending caps — the app has no other backend, no Docker, no web server. Android development tasks are build, test, and lint via Gradle; the proxy is a Node function deployed with the Firebase CLI (see `functions/README.md`).

## Build commands

| Task | Command |
|---|---|
| Build debug APK | `./gradlew assembleDebug` |
| Build release AAB | `./gradlew bundleRelease` |
| Run unit tests | `./gradlew testDebugUnitTest` |
| Lint | `./gradlew lintDebug` |
| Update version catalog | `./gradlew versionCatalogUpdate` |
| Deploy OpenAI proxy | `firebase deploy --only functions` (from repo root; see `functions/README.md`) |

## Testing

- For deep-link changes, consider `@DeeplinkTest` coverage.

## CI/CD

- **CI** (`.github/workflows/android.yml`): runs tests + `assembleDebug` on push/PR to `master`. On merge to `master`, creates a GitHub Release tagged `build-<shortSha>`. On PRs from the same repo, creates a prerelease tagged `pr-<PR#>-<shortSha>`.
- **Release** (`.github/workflows/release.yml`): triggered by push to `release` branch. Builds signed AAB, creates a Release tagged `v<appVersionName>`, publishes to Google Play internal track. **Bump `appVersionName` / `appVersionCode` in `app/build.gradle.kts` before merging to `release`; reusing a tag fails the release step.**
- Release signing uses env vars: `RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` (base64 keystore from `RELEASE_KEYSTORE_BASE64` secret in CI).
- Debug keystore is committed at `signing/keystore.jks` (password: `keystore`).
- The OpenAI key is **not** embedded in the app or CI. It lives only as a Firebase secret (`OPENAI_KEY`) used by the `functions/` proxy; keyless app builds reach OpenAI through that proxy (authenticated with an anonymous Firebase token). The hidden "Personalise AI" feature lets a user supply their own key at runtime for a direct call. See `functions/README.md`.
- Do not introduce secrets into source control.
