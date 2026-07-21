# Daily Macros

## Building, Signing and Shipping

- Bump `appVersionName` and `appVersionCode` in `app/build.gradle.kts` (release CI uses the same values for Git tag `v{versionName}`)
- Menu > Build > Generate Signed App Bundle or APK... > Android App Bundle > [use the production signing key] > release > Create
- Sign in to https://play.google.com/console with nomadworkz@gmail.com > Select Nomadworkz developer account
- Go to Test and release > Testing > Internal testing > Create new release
- Drag and drop the new aab file into the website
- Specify release name and release notes
- Tap Next > Save and publish

- Merging to the `release` branch creates the Git tag and GitHub Release in CI (see `AGENTS.md`)

./gradlew versionCatalogUpdate

## Pending doc/text updates for hidden features

Some features are currently hidden, so their data handling was deliberately
left out of the user-facing privacy text. When a feature below is unhidden or
shipped, make the listed changes so the disclosures stay accurate.

### Personalise AI (bring-your-own OpenAI key)

- `PRIVACY.md`, "Data sent to OpenAI" section: change the opening of the routing
  paragraph from "These requests are routed…" back to "By default these
  requests are routed…", and re-add the paragraph explaining that supplying a
  personal key sends requests directly to OpenAI, bypassing the developer's
  proxy, and that the key is stored encrypted on-device and never backed up
  off the device.
- `PRIVACY.md`, "Optional cloud backup" section: re-add "Your encrypted API key
  is never included in any backup."
- Play Console Data safety form: note the direct-to-OpenAI path for BYO-key users.

### Weekly insights

- `PRIVACY.md`, "Data sent to OpenAI" section: change the intro to "When you add
  a meal, or request weekly insights, the app sends…" and re-add the bullet that
  your recent food diary (~2 weeks of entries) and nutritional targets are sent
  to OpenAI for insights.
- `features/settings/.../res/values/strings.xml`, `settings_privacy_disclosure`:
  re-add "and, for weekly insights, your recent food diary" to the sentence
  about what is sent to OpenAI.
- Play Console Data safety form: confirm recent-diary + targets are covered.

### Auto photo detection (scan camera folder)

- `PRIVACY.md`, "Permissions" section: expand the `READ_MEDIA_IMAGES` bullet to
  note that, when auto-detection is enabled, the app scans new photos in your
  Camera folder to suggest them as candidate meals (and that those photos are
  only analysed when the feature is used).
- Play Console Data safety form / permissions declaration: cover the camera-folder
  scan use of `READ_MEDIA_IMAGES`.

