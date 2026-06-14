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

## Migrate existing images to new folder layout (one-time, after updating to this layout)

Full-size photos moved to cache, thumbnails moved to `files/thumbnails/`:

```powershell
# Windows – pull thumbnails from device, push back to new location
cmd /c 'adb exec-out run-as dev.gaborbiro.dailymacros tar -cf - files/public > "%USERPROFILE%\Desktop\public-backup.tar"'
```

```bash
# On-device migration via adb shell
adb shell "run-as dev.gaborbiro.dailymacros sh -c '
  mkdir -p cache/photos files/thumbnails
  for f in files/public/*; do
    name=\$(basename \"\$f\")
    case \"\$name\" in
      *-thumb*) mv \"\$f\" files/thumbnails/ ;;
      *)        mv \"\$f\" cache/photos/ ;;
    esac
  done
  rmdir files/public 2>/dev/null || true
'"
```
