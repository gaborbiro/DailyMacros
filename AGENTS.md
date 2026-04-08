# AGENTS.md

## Cursor Cloud specific instructions

This is a native Android app (Daily Macros) — there is no backend, no Docker, and no web server. Development tasks are build, test, and lint via Gradle.

### Environment prerequisites

- **JDK 17** (`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`)
- **Android SDK** at `/opt/android-sdk` with platform `android-36` and `build-tools;36.0.0`
- `local.properties` in the repo root must contain `sdk.dir=/opt/android-sdk`

### Key commands

All commands require `JAVA_HOME` and `ANDROID_HOME` to be set (already in `~/.bashrc`).

| Task | Command |
|---|---|
| Build debug APK | `./gradlew assembleDebug` |
| Build minified APK (R8, debug keystore) | `./gradlew assembleMinifiedDebug` |
| Build release AAB | `./gradlew bundleRelease` |
| Run unit tests | `./gradlew testDebugUnitTest` |
| Lint | `./gradlew lintDebug` |
| Update version catalog | `./gradlew versionCatalogUpdate` |

### CI/CD

- **CI** (`.github/workflows/android.yml`): runs on push/PR to `master` — tests, `assembleMinifiedDebug` (R8/ProGuard + shrink, signed with debug keystore); on push to `master` only, creates a GitHub Release with tag `build-<shortSha>` and that APK.
- **Release** (`.github/workflows/release.yml`): triggered by **push to `release`** (e.g. after merging a PR into that branch; protect `release` from direct pushes in GitHub). Builds signed release AAB, creates a GitHub Release with tag **`v` + `appVersionName`** from **`app/build.gradle.kts`** (via `:app:writeAppReleaseVersionNameFile`), publishes to Google Play internal testing track. Bump **`appVersionName`** / **`appVersionCode`** in **`app/build.gradle.kts`** before merging to `release`; reusing a tag will fail the release step.
- Release signing reads keystore from env vars (`RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`). In CI, the base64-encoded keystore is decoded from the `RELEASE_KEYSTORE_BASE64` secret.

### Notes

- The `CHATGPT_API_KEY` env var (or `local.properties` entry) is optional — the build succeeds without it, but AI food-recognition features will not work at runtime.
- This is a purely client-side Android app; there is no emulator in the Cloud Agent VM, so "running the app" means building the APK. Manual UI testing requires an Android 12+ device/emulator.
- Debug signing keystore is committed at `signing/keystore.jks` (password: `keystore`).
- Release signing uses a separate production keystore, never committed to the repo.
