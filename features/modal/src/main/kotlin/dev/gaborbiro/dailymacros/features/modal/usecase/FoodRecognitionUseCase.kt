package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.utils.inputStreamToBase64
import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ForImageUploadChatGpt
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import java.util.Locale
import javax.inject.Inject

class FoodRecognitionUseCase @Inject constructor(
    private val imageStore: ImageStore,
    @ForImageUploadChatGpt private val chatGPTRepository: ChatGPTRepository,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun execute(imageFilenames: List<String>): RecognisedFood {
        val base64Images = imageFilenames.map {
            val inputStream = imageStore.open(filename = it, thumbnail = false)
            inputStreamToBase64(inputStream)
        }
        val response = chatGPTRepository.recogniseFood(
            request = FoodRecognitionRequest(
                base64Images = base64Images,
                customisations = settingsRepository.getEffectiveCustomisations(),
                phoneLanguage = Locale.getDefault().getDisplayLanguage(Locale.ENGLISH),
            )
        )

        return RecognisedFood(
            title = response.title,
            warning = response.error,
        )
    }
}

