package dev.gaborbiro.dailymacros.features.shared.mealreminders

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.MealReminderSlotHistory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.toKotlinInstant

/**
 * Covers the passes that post nothing: posting logs to Firebase Analytics, which isn't available
 * in unit tests. What gets posted when is covered by [MealReminderPlannerTest] and
 * [MealReminderScheduleTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class RunMealReminderPassUseCaseTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = FakeMealReminderRepository()
    private val alarmManager = shadowOf(context.getSystemService(AlarmManager::class.java))

    private val useCase = RunMealReminderPassUseCase(
        appContext = context,
        getMealReminderInputsUseCase = GetMealReminderInputsUseCase(
            recordsRepository = FakeRecordsRepository(),
            settingsRepository = FakeSettingsRepository(),
            loggedDayRuleProvider = { LoggedDayRule.AnyRecord },
        ),
        mealReminderRepository = repository,
        notifier = MealReminderNotifier(context, UnusedModalNavigator),
        analyticsLogger = AnalyticsLogger(sinks = emptySet()),
    )

    // Breakfast's default reminder is at 08:00, so nothing is due yet.
    private val beforeBreakfast = ZonedDateTime.of(2024, 2, 7, 6, 0, 0, 0, ZoneId.systemDefault())

    @Test
    fun `with reminders off it cancels the alarm and records why`() = runTest {
        repository.setEnabled(true)
        useCase.execute(now = beforeBreakfast, trigger = "app start")
        assertNotNull(alarmManager.nextScheduledAlarm)

        repository.setEnabled(false)
        useCase.execute(now = beforeBreakfast, trigger = "settings")

        assertNull(alarmManager.nextScheduledAlarm)
        assertEquals("settings: reminders off", repository.getLastPass()?.summary)
        assertNull(repository.getLastPass()?.nextWakeUp)
    }

    @Test
    fun `with nothing due it saves the history and schedules the next check`() = runTest {
        repository.setEnabled(true)

        useCase.execute(now = beforeBreakfast, trigger = "app start")

        val lastPass = repository.getLastPass()!!
        assertEquals(beforeBreakfast.toInstant().toKotlinInstant(), lastPass.at)
        assertEquals("app start: nothing due", lastPass.summary)
        val nextWakeUp = lastPass.nextWakeUp!!
        assertTrue(nextWakeUp > lastPass.at)
        assertEquals(nextWakeUp.toEpochMilliseconds(), alarmManager.nextScheduledAlarm?.triggerAtMs)
        assertEquals(
            setOf("breakfast", "lunch", "dinner").associateWith { MealReminderSlotHistory() },
            repository.slotHistory,
        )
    }

    @Test
    fun `a due reminder is not posted while notifications are blocked`() = runTest {
        repository.setEnabled(true)
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)

        useCase.execute(now = beforeBreakfast.withHour(8), trigger = "alarm")

        assertEquals("alarm: breakfast due but notifications blocked", repository.getLastPass()?.summary)
        assertEquals(MealReminderSlotHistory(), repository.slotHistory["breakfast"])
    }
}
