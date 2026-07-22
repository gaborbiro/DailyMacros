package dev.gaborbiro.dailymacros.repositories.settings.domain.model

/** Quick date-range choices offered in the PDF export dialog. Persisted as the last-used range. */
enum class DateRangePreset {
    TODAY,
    THIS_WEEK,
    LAST_7_DAYS,
    LAST_WEEK,
    THIS_MONTH,
    LAST_30_DAYS,
    LAST_MONTH,
}
