package dev.gaborbiro.dailymacros.repositories.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.repositories.settings.domain.MealReminderRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.MealReminderLastPass
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.MealReminderSlotHistory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant

/**
 * Keeps meal reminder state in its own prefs file. The file name and keys predate this class
 * (they were written from the features module), so they must not change.
 */
@Singleton
class MealReminderRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : MealReminderRepository {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    override fun setEnabled(enabled: Boolean) = prefs.edit { putBoolean(KEY_ENABLED, enabled) }

    override fun getSlotHistory(slotKeys: Collection<String>): Map<String, MealReminderSlotHistory> =
        slotKeys.associateWith { slotKey ->
            MealReminderSlotHistory(
                lastRemindedAt = prefs.getInstant(slotKey.field(KEY_LAST_REMINDED_AT)),
                consecutiveIgnored = prefs.getInt(slotKey.field(KEY_CONSECUTIVE_IGNORED), 0),
                awaitingOutcome = prefs.getBoolean(slotKey.field(KEY_AWAITING_OUTCOME), false),
            )
        }

    override fun saveSlotHistory(history: Map<String, MealReminderSlotHistory>) = prefs.edit {
        history.forEach { (slotKey, slotHistory) ->
            val remindedAt = slotHistory.lastRemindedAt
            if (remindedAt != null) {
                putLong(slotKey.field(KEY_LAST_REMINDED_AT), remindedAt.toEpochMilliseconds())
            } else {
                remove(slotKey.field(KEY_LAST_REMINDED_AT))
            }
            putInt(slotKey.field(KEY_CONSECUTIVE_IGNORED), slotHistory.consecutiveIgnored)
            putBoolean(slotKey.field(KEY_AWAITING_OUTCOME), slotHistory.awaitingOutcome)
        }
    }

    override fun getLastPass(): MealReminderLastPass? {
        val at = prefs.getInstant(KEY_LAST_PASS_AT) ?: return null
        return MealReminderLastPass(
            at = at,
            summary = prefs.getString(KEY_LAST_PASS_SUMMARY, null).orEmpty(),
            nextWakeUp = prefs.getInstant(KEY_NEXT_WAKE_UP_AT),
        )
    }

    override fun setLastPass(lastPass: MealReminderLastPass) = prefs.edit {
        putLong(KEY_LAST_PASS_AT, lastPass.at.toEpochMilliseconds())
        putString(KEY_LAST_PASS_SUMMARY, lastPass.summary)
        lastPass.nextWakeUp
            ?.let { putLong(KEY_NEXT_WAKE_UP_AT, it.toEpochMilliseconds()) }
            ?: remove(KEY_NEXT_WAKE_UP_AT)
    }

    private fun String.field(field: String) = "${this}_$field"

    private fun SharedPreferences.getInstant(key: String): Instant? =
        getLong(key, -1L).takeIf { it >= 0 }?.let(Instant::fromEpochMilliseconds)

    private companion object {
        const val PREFS_NAME = "meal_reminders"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LAST_REMINDED_AT = "last_reminded_at"
        private const val KEY_CONSECUTIVE_IGNORED = "consecutive_ignored"
        private const val KEY_AWAITING_OUTCOME = "awaiting_outcome"
        private const val KEY_LAST_PASS_AT = "last_pass_at"
        private const val KEY_LAST_PASS_SUMMARY = "last_pass_summary"
        private const val KEY_NEXT_WAKE_UP_AT = "next_wake_up_at"
    }
}
