package dev.gaborbiro.dailymacros.features.overview.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.gaborbiro.dailymacros.features.overview.OverviewPrefs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class ResolveOverviewCoachMarkUseCaseTest {

    @Before
    fun clearPrefs() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("overview_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `does not trigger when item count is zero`() {
        val prefs = OverviewPrefs(ApplicationProvider.getApplicationContext())
        prefs.showCoachMark = true
        val useCase = ResolveOverviewCoachMarkUseCase(prefs)
        assertFalse(useCase.execute(0))
        assertTrue(prefs.showCoachMark)
    }

    @Test
    fun `triggers once and clears pref`() {
        val prefs = OverviewPrefs(ApplicationProvider.getApplicationContext())
        prefs.showCoachMark = true
        val useCase = ResolveOverviewCoachMarkUseCase(prefs)
        assertTrue(useCase.execute(2))
        assertFalse(prefs.showCoachMark)
    }

    @Test
    fun `does not trigger when pref already off`() {
        val prefs = OverviewPrefs(ApplicationProvider.getApplicationContext())
        prefs.showCoachMark = false
        val useCase = ResolveOverviewCoachMarkUseCase(prefs)
        assertFalse(useCase.execute(2))
    }
}
