package dev.gaborbiro.dailymacros.features.shared.mealreminders

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.MealReminderLastPass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class GetMealReminderStatusUseCaseTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = FakeMealReminderRepository()
    private val useCase = GetMealReminderStatusUseCase(repository, MealReminderNotifier(context, UnusedModalNavigator))

    @Test
    fun `reports the switch, the notification permission and the last pass`() {
        val at = ZonedDateTime.of(2024, 2, 7, 6, 0, 0, 0, ZoneId.systemDefault())
        repository.setEnabled(true)
        repository.setLastPass(
            MealReminderLastPass(
                at = at.toInstant().toKotlinInstant(),
                summary = "app start: nothing due",
                nextWakeUp = at.plusHours(2).toInstant().toKotlinInstant(),
            )
        )

        val status = useCase.execute()

        assertEquals(
            MealReminderStatus(
                enabled = true,
                notificationsAllowed = true,
                lastPass = MealReminderStatus.LastPass(at, "app start: nothing due", at.plusHours(2)),
            ),
            status,
        )
    }

    @Test
    fun `reports blocked notifications and a pass that never ran`() {
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)

        val status = useCase.execute()

        assertFalse(status.enabled)
        assertFalse(status.notificationsAllowed)
        assertNull(status.lastPass)
    }

    @Test
    fun `the switch set by SetMealRemindersEnabledUseCase is reported back`() {
        SetMealRemindersEnabledUseCase(repository).execute(true)

        assertTrue(useCase.execute().enabled)

        SetMealRemindersEnabledUseCase(repository).execute(false)

        assertFalse(useCase.execute().enabled)
    }
}
