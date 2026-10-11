package dev.gaborbiro.dailymacros.repositories.settings.domain.model

/** A device timezone change captured independently of any logged meal, so the jet-lag
 *  advisory (see `OverviewUiMapper`) can detect a shift even on a day with no records. */
data class TimezoneEvent(
    val epochMs: Long,
    val zoneId: String,
)
