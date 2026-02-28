package dev.gaborbiro.dailymacros.features.settings

/**
 * Provides app version and user id for the settings screen footer.
 * Implemented by the app module (e.g. from BuildConfig + AppPrefs).
 */
interface SettingsAppInfo {
    val versionLabel: String
}
