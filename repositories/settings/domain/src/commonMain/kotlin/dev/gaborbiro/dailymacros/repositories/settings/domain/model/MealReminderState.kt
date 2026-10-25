package dev.gaborbiro.dailymacros.repositories.settings.domain.model

import kotlin.time.Instant

/** What one meal slot's reminder has done recently. */
data class MealReminderSlotHistory(
    val lastRemindedAt: Instant? = null,
    /** Reminders in a row that ended without the slot getting logged. */
    val consecutiveIgnored: Int = 0,
    /** The last reminder went out and its slot is neither logged nor over yet. */
    val awaitingOutcome: Boolean = false,
)

/** What a reminder pass did and when the next one is due. */
data class MealReminderLastPass(
    val at: Instant,
    val summary: String,
    val nextWakeUp: Instant?,
)
