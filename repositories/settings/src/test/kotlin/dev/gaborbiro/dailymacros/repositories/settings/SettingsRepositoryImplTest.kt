package dev.gaborbiro.dailymacros.repositories.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class SettingsRepositoryImplTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun newRepository() = SettingsRepositoryImpl(context, SettingsMapper(Gson()))

    @Test
    fun `recorded timezone events round trip in chronological order`() {
        val repository = newRepository()
        val now = 1_000_000_000_000L

        repository.recordTimezoneEvent("Europe/Lisbon", now)
        repository.recordTimezoneEvent("Australia/Sydney", now + 1_000L)

        val events = repository.getRecentTimezoneEvents()

        assertEquals(2, events.size)
        assertEquals("Europe/Lisbon", events[0].zoneId)
        assertEquals("Australia/Sydney", events[1].zoneId)
    }

    @Test
    fun `refiring the same zone is a no-op`() {
        val repository = newRepository()
        val now = 1_000_000_000_000L

        repository.recordTimezoneEvent("Europe/Lisbon", now)
        repository.recordTimezoneEvent("Europe/Lisbon", now + 1_000L)

        assertEquals(1, repository.getRecentTimezoneEvents().size)
    }

    @Test
    fun `events older than 14 days are pruned on the next write`() {
        val repository = newRepository()
        val now = 1_000_000_000_000L
        val fifteenDaysAgo = now - Duration.ofDays(15).toMillis()

        repository.recordTimezoneEvent("Australia/Sydney", fifteenDaysAgo)
        repository.recordTimezoneEvent("Europe/Lisbon", now)

        val events = repository.getRecentTimezoneEvents()

        assertEquals(1, events.size)
        assertEquals("Europe/Lisbon", events.single().zoneId)
    }

    @Test
    fun `timezone change tracking is opt-in and defaults to off`() {
        val repository = newRepository()

        assertEquals(false, repository.getTimezoneChangeTrackingEnabled())

        repository.setTimezoneChangeTrackingEnabled(true)

        assertTrue(repository.getTimezoneChangeTrackingEnabled())
    }
}
