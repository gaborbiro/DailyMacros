package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.features.modal.sha256
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toApiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class RequestCacheConsistencyTest {

    @Test
    fun `first two input parts are identical across both request types`() {
        val images = listOf("data:image/jpeg;base64,abc123", "data:image/jpeg;base64,def456")

        val foodRecognitionRequest = FoodRecognitionRequest(base64Images = images).toApiModel()
        val nutrientAnalysisRequest = NutrientAnalysisRequest(
            base64Images = images,
            title = "irrelevant",
            description = "irrelevant",
        ).toApiModel()

        val foodRecognitionHash = foodRecognitionRequest.input.take(2).joinToString().sha256()
        val nutrientAnalysisHash = nutrientAnalysisRequest.input.take(2).joinToString().sha256()

        assertEquals(
            "The first two parts (system prompt + images) must be identical for prompt caching",
            foodRecognitionHash,
            nutrientAnalysisHash,
        )
    }
}
