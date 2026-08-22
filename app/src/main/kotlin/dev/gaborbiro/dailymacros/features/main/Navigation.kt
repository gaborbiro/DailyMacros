package dev.gaborbiro.dailymacros.features.main


const val OVERVIEW_ROUTE = "overview"
const val SETTINGS_ROUTE = "settings"
const val SETTINGS_HIGHLIGHT_ROW_ARG = "hr"
const val TRENDS_ROUTE = "trends"
const val TRENDS_SCROLL_EPOCH_DAY_ARG = "trendsScrollEpochDay"
const val TRENDS_TIMESCALE_ARG = "trendsTimescale"

/**
 * The exact route pattern the Settings destination is registered with in the nav graph
 * (placeholder included). Needed as a `popUpTo` target so that navigating to Settings from
 * a new entry point (e.g. a notification deep link) replaces any existing Settings back-stack
 * entry instead of stacking a second one on top of it - Settings can be reached with a
 * different highlighted row each time, so the route strings themselves never match exactly.
 */
const val SETTINGS_ROUTE_PATTERN = "$SETTINGS_ROUTE?$SETTINGS_HIGHLIGHT_ROW_ARG={$SETTINGS_HIGHLIGHT_ROW_ARG}"

/** See TRENDS_ROUTE_PATTERN in features/common's Navigation.kt for what these args are for. */
const val TRENDS_ROUTE_PATTERN =
    "$TRENDS_ROUTE?$TRENDS_SCROLL_EPOCH_DAY_ARG={$TRENDS_SCROLL_EPOCH_DAY_ARG}&$TRENDS_TIMESCALE_ARG={$TRENDS_TIMESCALE_ARG}"
