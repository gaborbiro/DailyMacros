package dev.gaborbiro.dailymacros.features.common

const val OVERVIEW_ROUTE = "overview"
const val SETTINGS_ROUTE = "settings"
const val SETTINGS_HIGHLIGHT_ROW_ARG = "hr"

/**
 * The exact route pattern the Settings destination is registered with in the nav graph
 * (placeholder included). Needed as a `popUpTo` target so that navigating to Settings from
 * a new entry point (e.g. a notification deep link) replaces any existing Settings back-stack
 * entry instead of stacking a second one on top of it - Settings can be reached with a
 * different highlighted row each time, so the route strings themselves never match exactly.
 */
const val SETTINGS_ROUTE_PATTERN = "$SETTINGS_ROUTE?$SETTINGS_HIGHLIGHT_ROW_ARG={$SETTINGS_HIGHLIGHT_ROW_ARG}"
const val TRENDS_ROUTE = "trends"

/**
 * Key used to pass a target date back from Trends to Overview via the Overview back-stack
 * entry's SavedStateHandle, when the user taps a chart data point - see TrendsScreen.kt and
 * OverviewScreen.kt. A plain nav argument doesn't fit here: Overview is already on the back
 * stack (Trends was pushed on top of it, not launched as a fresh instance), so returning to it
 * via popBackStack() re-enters the *same* screen/ViewModel instance rather than a new
 * destination that could take a route argument.
 */
const val OVERVIEW_SCROLL_TO_EPOCH_DAY_KEY = "scrollToEpochDay"
const val ONBOARDING_ROUTE = "onboarding"
const val PAYWALL_ROUTE = "paywall"

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
    SUBSCRIPTION,
}
