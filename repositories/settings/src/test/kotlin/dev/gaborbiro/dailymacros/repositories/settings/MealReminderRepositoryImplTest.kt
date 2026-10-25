package dev.gaborbiro.dailymacros.repositories.settings

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.MealReminderLastPass
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.MealReminderSlotHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class MealReminderRepositoryImplTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun newRepository() = MealReminderRepositoryImpl(context)

    @Test
    fun `reminders are off and history is empty on a fresh install`() {
        val repository = newRepository()

        assertFalse(repository.isEnabled())
        assertEquals(
            mapOf("lunch" to MealReminderSlotHistory()),
            repository.getSlotHistory(listOf("lunch")),
        )
        assertNull(repository.getLastPass())
    }

    @Test
    fun `values round trip`() {
        val history = mapOf(
            "breakfast" to MealReminderSlotHistory(
                lastRemindedAt = Instant.fromEpochMilliseconds(1_000L),
                consecutiveIgnored = 2,
                awaitingOutcome = true,
            ),
            "dinner" to MealReminderSlotHistory(),
        )
        val lastPass = MealReminderLastPass(
            at = Instant.fromEpochMilliseconds(2_000L),
            summary = "settings: nothing due",
            nextWakeUp = Instant.fromEpochMilliseconds(3_000L),
        )

        newRepository().apply {
            setEnabled(true)
            saveSlotHistory(history)
            setLastPass(lastPass)
        }

        val reloaded = newRepository()
        assertTrue(reloaded.isEnabled())
        assertEquals(history, reloaded.getSlotHistory(history.keys))
        assertEquals(lastPass, reloaded.getLastPass())
    }

    @Test
    fun `clearing a slot's reminder time and the next wake-up removes them`() {
        val repository = newRepository()
        repository.saveSlotHistory(mapOf("lunch" to MealReminderSlotHistory(lastRemindedAt = Instant.fromEpochMilliseconds(1_000L))))
        repository.setLastPass(MealReminderLastPass(Instant.fromEpochMilliseconds(2_000L), "a", Instant.fromEpochMilliseconds(3_000L)))

        repository.saveSlotHistory(mapOf("lunch" to MealReminderSlotHistory(lastRemindedAt = null)))
        repository.setLastPass(MealReminderLastPass(Instant.fromEpochMilliseconds(4_000L), "b", nextWakeUp = null))

        assertNull(repository.getSlotHistory(listOf("lunch")).getValue("lunch").lastRemindedAt)
        assertNull(repository.getLastPass()?.nextWakeUp)
    }

    /** Settings saved before the repository existed (same file, same keys) must still be read. */
    @Test
    fun `reads settings stored under the original file name and keys`() {
        context.getSharedPreferences("meal_reminders", Context.MODE_PRIVATE).edit {
            putBoolean("enabled", true)
            putLong("lunch_last_reminded_at", 1_000L)
            putInt("lunch_consecutive_ignored", 3)
            putBoolean("lunch_awaiting_outcome", true)
            putLong("last_pass_at", 2_000L)
            putString("last_pass_summary", "app start: posted lunch")
            putLong("next_wake_up_at", 3_000L)
        }

        val repository = newRepository()

        assertTrue(repository.isEnabled())
        assertEquals(
            MealReminderSlotHistory(
                lastRemindedAt = Instant.fromEpochMilliseconds(1_000L),
                consecutiveIgnored = 3,
                awaitingOutcome = true,
            ),
            repository.getSlotHistory(listOf("lunch")).getValue("lunch"),
        )
        assertEquals(
            MealReminderLastPass(
                at = Instant.fromEpochMilliseconds(2_000L),
                summary = "app start: posted lunch",
                nextWakeUp = Instant.fromEpochMilliseconds(3_000L),
            ),
            repository.getLastPass(),
        )
    }
}
