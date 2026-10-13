package dev.gaborbiro.dailymacros.features.shared.mealreminders

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** The meal reminder on/off switch and each slot's [SlotReminderHistory]. */
@Singleton
class MealReminderPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs = context.applicationContext.getSharedPreferences("meal_reminders", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    /** What the last reminder pass did, for the Settings > Debug readout. */
    var lastPass: LastPass?
        get() {
            val at = prefs.getLong(KEY_LAST_PASS_AT, -1L).takeIf { it >= 0 } ?: return null
            return LastPass(
                at = at.toZonedDateTime(),
                summary = prefs.getString(KEY_LAST_PASS_SUMMARY, null).orEmpty(),
                nextWakeUp = prefs.getLong(KEY_NEXT_WAKE_UP_AT, -1L).takeIf { it >= 0 }?.toZonedDateTime(),
            )
        }
        set(value) = prefs.edit {
            if (value == null) {
                remove(KEY_LAST_PASS_AT)
                remove(KEY_LAST_PASS_SUMMARY)
                remove(KEY_NEXT_WAKE_UP_AT)
            } else {
                putLong(KEY_LAST_PASS_AT, value.at.toInstant().toEpochMilli())
                putString(KEY_LAST_PASS_SUMMARY, value.summary)
                value.nextWakeUp
                    ?.let { putLong(KEY_NEXT_WAKE_UP_AT, it.toInstant().toEpochMilli()) }
                    ?: remove(KEY_NEXT_WAKE_UP_AT)
            }
        }

    data class LastPass(val at: ZonedDateTime, val summary: String, val nextWakeUp: ZonedDateTime?)

    fun history(): Map<MealSlot, SlotReminderHistory> = MealSlot.entries.associateWith { slot ->
        SlotReminderHistory(
            lastRemindedAt = prefs.getLong(slot.key(KEY_LAST_REMINDED_AT), -1L)
                .takeIf { it >= 0 }
                ?.toZonedDateTime(),
            consecutiveIgnored = prefs.getInt(slot.key(KEY_CONSECUTIVE_IGNORED), 0),
            awaitingOutcome = prefs.getBoolean(slot.key(KEY_AWAITING_OUTCOME), false),
        )
    }

    fun saveHistory(history: Map<MealSlot, SlotReminderHistory>) = prefs.edit {
        history.forEach { (slot, slotHistory) ->
            val remindedAt = slotHistory.lastRemindedAt
            if (remindedAt != null) {
                putLong(slot.key(KEY_LAST_REMINDED_AT), remindedAt.toInstant().toEpochMilli())
            } else {
                remove(slot.key(KEY_LAST_REMINDED_AT))
            }
            putInt(slot.key(KEY_CONSECUTIVE_IGNORED), slotHistory.consecutiveIgnored)
            putBoolean(slot.key(KEY_AWAITING_OUTCOME), slotHistory.awaitingOutcome)
        }
    }

    private fun MealSlot.key(field: String) = "${name.lowercase()}_$field"

    private fun Long.toZonedDateTime() = ZonedDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_LAST_REMINDED_AT = "last_reminded_at"
        const val KEY_CONSECUTIVE_IGNORED = "consecutive_ignored"
        const val KEY_AWAITING_OUTCOME = "awaiting_outcome"
        const val KEY_LAST_PASS_AT = "last_pass_at"
        const val KEY_LAST_PASS_SUMMARY = "last_pass_summary"
        const val KEY_NEXT_WAKE_UP_AT = "next_wake_up_at"
    }
}
