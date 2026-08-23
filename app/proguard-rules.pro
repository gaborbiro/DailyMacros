# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# PdfBox-Android references the optional JPEG 2000 codec (com.gemalto.jp2)
# from JPXFilter, but that dependency is not bundled. We don't decode JP2
# images, so it's safe to tell R8 to ignore the missing references.
-dontwarn com.gemalto.jp2.**

# SettingsMapper persists Daily Targets by Gson-serializing the domain Targets/Target
# classes but deserializing into a separate TargetsJson shape (for null-safe parsing of
# older/corrupted prefs). Without keeping field names, R8 renames each class's fields
# independently, so the JSON keys written by one no longer match the keys read by the
# other and every target silently resets. Keep field names so the two stay in sync.
-keepclassmembers class dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets { *; }
-keepclassmembers class dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target { *; }
-keepclassmembers class dev.gaborbiro.dailymacros.repositories.settings.SettingsMapper$TargetsJson { *; }
