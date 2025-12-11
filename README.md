# Daily Macros

## Building, Signing and Shipping

- Bump versionName and versionCode in app/build.gradle.kts
- Menu > Build > Generate Signed App Bundle or APK... > Android App Bundle > [use the production signing key] > release > Create
- Sign in to https://play.google.com/console with nomadworkz@gmail.com > Select Nomadworkz developer account
- Go to Test and release > Testing > Internal testing > Create new release
- Drag and drop the new aab file into the website
- Specify release name and release notes
- Tap Next > Save and publish

- Don't forget to git tag the commit
