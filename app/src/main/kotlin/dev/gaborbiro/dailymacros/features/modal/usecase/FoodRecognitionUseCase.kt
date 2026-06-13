package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.utils.inputStreamToBase64
import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.repositories.chatgpt.di.ForImageUploadChatGpt
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

class FoodRecognitionUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val imageStore: ImageStore,
    @ForImageUploadChatGpt private val chatGPTRepository: ChatGPTRepository,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun execute(images: List<String>): RecognisedFood {
        val base64Images = images.map {
            val inputStream = imageStore.open(it, thumbnail = false)
            inputStreamToBase64(inputStream)
        }
        val response = chatGPTRepository.recogniseFood(
            request = FoodRecognitionRequest(
                base64Images = base64Images,
                customizations = settingsRepository.getPromptCustomizations(),
                phoneLanguage = Locale.getDefault().getDisplayLanguage(Locale.ENGLISH),
            )
        )
        return RecognisedFood(
            title = response.title,
        )
    }
}

