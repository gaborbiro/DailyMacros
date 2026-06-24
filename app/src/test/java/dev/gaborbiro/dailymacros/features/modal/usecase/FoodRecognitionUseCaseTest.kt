package dev.gaborbiro.dailymacros.features.modal.usecase

import android.graphics.Bitmap
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysis
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
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

        override fun getRecognitionPromptSegments() = emptyList<PromptSegment>()
        override fun getAnalysisPromptSegments() = emptyList<PromptSegment>()
    }

    private val fakeSettingsRepository = object : SettingsRepository {
        override fun getTargets() = Targets(
            calories = Target(enabled = false),
            protein = Target(enabled = false),
            salt = Target(enabled = false),
            fat = Target(enabled = false),
            carbs = Target(enabled = false),
            fibre = Target(enabled = false),
            ofWhichSaturated = Target(enabled = false),
            ofWhichSugar = Target(enabled = false),
        )

        override fun setTargets(targets: Targets) = Unit
        override fun getDiaryDayStartHour(): Int = 0
        override fun setDiaryDayStartHour(hourOfDay: Int) = Unit
        override fun getPromptCustomizations(): Map<String, String> = emptyMap()
        override fun setPromptCustomizations(values: Map<String, String>) = Unit
        override fun getPromptVersions() = emptyList<PromptVersion>()
        override fun savePromptVersion(customizations: Map<String, String>) = PromptVersion(1, 0L, emptyMap())
        override fun deletePromptVersion(version: Int) {}
        override fun clearPromptCustomizations() = Unit
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
