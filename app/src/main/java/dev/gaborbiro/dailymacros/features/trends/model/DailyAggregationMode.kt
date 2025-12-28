package dev.gaborbiro.dailymacros.features.trends.model

enum class DailyAggregationMode {
    CALENDAR_DAYS,  // every calendar day counts (missing = 0)
    LOGGED_DAYS,    // only days with any data
    QUALIFIED_DAYS, // only days >= threshold
}