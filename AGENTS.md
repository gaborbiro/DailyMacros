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
| Run unit tests | `./gradlew testDebugUnitTest` |
| Lint | `./gradlew lintDebug` |
| Update version catalog | `./gradlew versionCatalogUpdate` |

### Known issues

- The `:repositories:chatgpt:testDebugUnitTest` task has a pre-existing compilation error: the test file imports `dev.gaborbiro.dailymacros.features.modal.sha256` and `junit` but the module's `build.gradle.kts` does not declare `testImplementation` dependencies for JUnit or a dependency on the `:app` module. To run all other unit tests, exclude it: `./gradlew testDebugUnitTest -x :repositories:chatgpt:testDebugUnitTest`.

### Notes

- The `CHATGPT_API_KEY` env var (or `local.properties` entry) is optional — the build succeeds without it, but AI food-recognition features will not work at runtime.
- This is a purely client-side Android app; there is no emulator in the Cloud Agent VM, so "running the app" means building the APK. Manual UI testing requires an Android 12+ device/emulator.
- Debug signing keystore is committed at `signing/keystore.jks` (password: `keystore`).
