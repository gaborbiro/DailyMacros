package dev.gaborbiro.dailymacros.features.modal.usecase

import android.graphics.Bitmap
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysis
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class FoodRecognitionUseCaseTest {

    private class FakeImageStore : ImageStore {
        override suspend fun open(filename: String, thumbnail: Boolean) =
            ByteArrayInputStream(ByteArray(0))

        override suspend fun read(filename: String, thumbnail: Boolean) = null

        override suspend fun write(filename: String, bitmap: Bitmap) = Unit

        override suspend fun delete(filename: String) = Unit
    }

    private class FakeChatGpt : ChatGPTRepository {
        override suspend fun recogniseFood(request: FoodRecognitionRequest) = FoodRecognitionResult(
            title = "Oats",
            cachedTokens = 12,
        )

        override suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysis =
            error("unused")

        override fun getRecognitionPromptSegments() = emptyList<dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment>()
        override fun getAnalysisPromptSegments() = emptyList<dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment>()
        override fun getInsightsPromptSegments() = emptyList<dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment>()
        override fun getOngoingInsightsPromptSegments() = emptyList<dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment>()
        override suspend fun getWeeklyInsights(request: dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.WeeklyInsightsRequest): Map<String, String> = error("unused")
        override suspend fun getOngoingInsights(request: dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.OngoingInsightsRequest): String = error("unused")
    }

    private val fakeSettingsRepository = object : dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository {
        override fun getTargets() = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets(
            calories = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
            protein = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
            salt = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
            fat = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
            carbs = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
            fibre = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
            ofWhichSaturated = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
            ofWhichSugar = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
        )
        override fun setTargets(targets: dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets) = Unit
        override fun getDiaryDayStartHour(): Int = 0
        override fun setDiaryDayStartHour(hourOfDay: Int) = Unit
        override fun getPromptCustomizations(): Map<String, String> = emptyMap()
        override fun setPromptCustomizations(values: Map<String, String>) = Unit
        override fun clearPromptCustomizations() = Unit
        override fun getPromptVersions(type: String) = emptyList<dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion>()
        override fun savePromptVersion(type: String, customizations: Map<String, String>) = dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion(1, 0L, emptyMap())
        override fun deletePromptVersion(version: Int) = Unit
        override fun getApiKeyOverride(): String? = null
        override fun setApiKeyOverride(key: String) = Unit
        override fun clearApiKeyOverride() = Unit
    }

    @Test
    fun `maps api result to recognised food`() = runBlocking {
        val result = FoodRecognitionUseCase(FakeImageStore(), FakeChatGpt(), fakeSettingsRepository).execute(listOf("pic.jpg"))
        assertEquals("Oats", result.title)
    }
}
