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

    fun history(): Map<MealSlot, SlotReminderHistory> = MealSlot.entries.associateWith { slot ->
        SlotReminderHistory(
            lastRemindedAt = prefs.getLong(slot.key(KEY_LAST_REMINDED_AT), -1L)
                .takeIf { it >= 0 }
                ?.let { ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) },
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

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_LAST_REMINDED_AT = "last_reminded_at"
        const val KEY_CONSECUTIVE_IGNORED = "consecutive_ignored"
        const val KEY_AWAITING_OUTCOME = "awaiting_outcome"
    }
}
