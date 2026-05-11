package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.utils.inputStreamToBase64
import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.di.ForImageUploadChatGpt
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repositories.chatgpt.toDomainModel
import dev.gaborbiro.dailymacros.util.showTextNotification
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.random.Random
import javax.inject.Inject

class FoodRecognitionUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val imageStore: ImageStore,
    @ForImageUploadChatGpt private val chatGPTRepository: ChatGPTRepository,
) {

    suspend fun execute(images: List<String>): RecognisedFood {
        val response = try {
            val base64Images = images.map {
                val inputStream = imageStore.open(it, thumbnail = false)
                inputStreamToBase64(inputStream)
            }
            chatGPTRepository.recogniseFood(
                request = FoodRecognitionRequest(
                    base64Images = base64Images,
                )
            )
        } catch (apiError: ChatGPTApiError) {
            throw apiError
                .toDomainModel()
        }
        val cachedTokens = "Cached tokens: ${response.cachedTokens}"
        appContext.showTextNotification(Random(564).nextLong(), cachedTokens)
        return RecognisedFood(
            title = response.title,
            description = response.description,
        )
    }
}
