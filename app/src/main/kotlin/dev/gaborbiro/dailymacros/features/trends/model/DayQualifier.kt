package dev.gaborbiro.dailymacros.features.trends.model

enum class DayQualifier {
    ALL_CALENDAR_DAYS,   // every calendar day counts
    ONLY_LOGGED_DAYS,    // only days with at least 1 record
    ONLY_QUALIFIED_DAYS, // only days, where calories >= [AppPrefs.qualifyingCalorieThreshold]
}
