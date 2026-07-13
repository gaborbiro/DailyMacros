package dev.gaborbiro.dailymacros.features.shared

import android.content.Context
import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.CloudSyncProvider
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ErrorUiMapperTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val mapperWithApiKey get() = ErrorUiMapper(context, fakeSettings(apiKey = "sk-test"))
    private val mapperWithoutApiKey get() = ErrorUiMapper(context, fakeSettings(apiKey = null))

    @Test
    fun `mapErrorMessage check internet connection`() {
        val result = mapperWithoutApiKey.mapErrorMessage(
            DomainError.DisplayMessageToUser.CheckInternetConnection(cause = RuntimeException()),
            defaultMessage = "default",
        )
        assertTrue(result.contains("internet", ignoreCase = true))
    }

    @Test
    fun `mapErrorMessage operation failed uses default`() {
        assertEquals(
            "Couldn't save your entry",
            mapperWithoutApiKey.mapErrorMessage(
                DomainError.DisplayMessageToUser.OperationFailed(),
                defaultMessage = "Couldn't save your entry",
            ),
        )
    }

    @Test
    fun `mapErrorMessage force technical message always shows`() {
        assertEquals(
            "API quota exceeded",
            mapperWithoutApiKey.mapErrorMessage(
                DomainError.DisplayMessageToUser.ForceTechnicalMessage("API quota exceeded"),
                defaultMessage = "default",
            ),
        )
    }

    @Test
    fun `mapErrorMessage technical message shown when api key is unlocked`() {
        assertEquals(
            "API error detail",
            mapperWithApiKey.mapErrorMessage(
                DomainError.DisplayMessageToUser.TechnicalMessage("API error detail"),
                defaultMessage = "default",
            ),
        )
    }

    @Test
    fun `mapErrorMessage technical message falls back to default when no api key`() {
        assertEquals(
            "Couldn't get macros",
            mapperWithoutApiKey.mapErrorMessage(
                DomainError.DisplayMessageToUser.TechnicalMessage("API error detail"),
                defaultMessage = "Couldn't get macros",
            ),
        )
    }
}

private fun fakeSettings(apiKey: String?): SettingsRepository = object : SettingsRepository {
    override fun getTargets(): Targets = Targets(
        calories = Target(enabled = false),
        protein = Target(enabled = false),
        salt = Target(enabled = false),
        fat = Target(enabled = false),
        carbs = Target(enabled = false),
        fibre = Target(enabled = false),
        ofWhichSaturated = Target(enabled = false),
        ofWhichSugar = Target(enabled = false),
    )
    override fun setTargets(targets: Targets) {}
    override fun getDiaryDayStartHour(): Int = 0
    override fun setDiaryDayStartHour(hourOfDay: Int) {}
    override fun getPromptCustomisations(): Map<String, String> = emptyMap()
    override fun setPromptCustomisations(values: Map<String, String>) {}
    override fun clearPromptCustomisations() {}
    override fun getPromptVersions(type: String): List<PromptVersion> = emptyList()
    override fun savePromptVersion(type: String, customisations: Map<String, String>): PromptVersion =
        throw UnsupportedOperationException()
    override fun deletePromptVersion(version: Int) {}
    override fun getApiKeyOverride(): String? = apiKey
    override fun setApiKeyOverride(key: String) {}
    override fun clearApiKeyOverride()  {}
    override fun getCloudSyncProvider(): CloudSyncProvider = CloudSyncProvider.NONE
}
