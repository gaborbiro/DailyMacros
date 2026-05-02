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

## Backup and Restore

cmd /c 'adb exec-out run-as dev.gaborbiro.dailymacros tar -cf - files/public > "%USERPROFILE%\Desktop\public-backup.tar"'
tar -tf "$env:USERPROFILE\Desktop\public-backup.tar" | Select-Object -First 20
cmd /c 'adb exec-in run-as dev.gaborbiro.dailymacros sh -c "cat > public-backup.tar" < "%USERPROFILE%\Desktop\public-backup.tar"'

Extract it to restore_tmp folder. This will put them in restore_tmp/files/public subfolder
adb shell "run-as dev.gaborbiro.dailymacros sh -c 'tar -xf public-backup.tar -C .'"
