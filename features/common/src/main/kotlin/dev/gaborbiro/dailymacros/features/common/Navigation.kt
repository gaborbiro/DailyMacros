package dev.gaborbiro.dailymacros.features.common

const val OVERVIEW_ROUTE = "overview"
const val SETTINGS_ROUTE = "settings"
const val SETTINGS_HIGHLIGHT_ROW_ARG = "hr"
const val TRENDS_ROUTE = "trends"
const val ONBOARDING_ROUTE = "onboarding"

/**
 * Identifies a row in the Settings screen that can be scrolled to and highlighted,
 * e.g. when arriving from a notification or another screen that wants to draw
 * attention to a specific setting.
 */
enum class SettingsRowId {
    TARGETS,
    AUTO_BACKUP,
    BACKUP_NOW,
    PRIVACY_POLICY,
}
