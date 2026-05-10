package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysis
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.VariabilityMiningResult
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
            description = "Porridge",
            cachedTokens = 12,
        )

        override suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysis =
            error("unused")

        override suspend fun mineMealVariability(userMessageJson: String): VariabilityMiningResult =
            error("unused")
    }

    @Test
    fun `maps api result to recognised food`() = runBlocking {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val result = FoodRecognitionUseCase(ctx, FakeImageStore(), FakeChatGpt()).execute(listOf("pic.jpg"))
        assertEquals("Oats", result.title)
        assertEquals("Porridge", result.description)
    }
}
