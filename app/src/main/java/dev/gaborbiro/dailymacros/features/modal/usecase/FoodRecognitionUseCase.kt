package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.modal.ModalUIMapper
import dev.gaborbiro.dailymacros.features.modal.inputStreamToBase64
import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repo.chatgpt.toDomainModel
import dev.gaborbiro.dailymacros.util.showTextNotification
import kotlin.random.Random

internal class FoodRecognitionUseCase(
    private val appContext: Context,
    private val imageStore: ImageStore,
    private val chatGPTRepository: ChatGPTRepository,
    private val mapper: ModalUIMapper,
) {

    suspend fun execute(images: List<String>): RecognisedFood {
        val response = try {
            val base64Images = images.map {
                val inputStream = imageStore.open(it, thumbnail = false)
                inputStreamToBase64(inputStream)
            }
            chatGPTRepository.recogniseFood(
                request = mapper.mapToFoodRecognitionRequest(
                    base64Images = base64Images,
                )
            )
        } catch (apiError: ChatGPTApiError) {
            throw apiError
                .toDomainModel()
        }
        val cachedTokens = "Cached tokens: ${response.cachedTokens}"
        appContext.showTextNotification(Random(564).nextLong(), cachedTokens)
        return mapper.mapRecognisedFood(response)
    }
}
