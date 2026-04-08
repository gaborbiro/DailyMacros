# Daily Macros

## Building, Signing and Shipping

- Bump `app.versionName` and `app.versionCode` in `gradle.properties` (used by Gradle and the release workflow Git tag `v{versionName}`)
- Menu > Build > Generate Signed App Bundle or APK... > Android App Bundle > [use the production signing key] > release > Create
- Sign in to https://play.google.com/console with nomadworkz@gmail.com > Select Nomadworkz developer account
- Go to Test and release > Testing > Internal testing > Create new release
- Drag and drop the new aab file into the website
- Specify release name and release notes
- Tap Next > Save and publish

- Merging to the `release` branch creates the Git tag and GitHub Release in CI (see `AGENTS.md`)

./gradlew versionCatalogUpdate