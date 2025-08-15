package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repo.chatgpt.ChatGPTRepository
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repo.chatgpt.toDomainModel
import dev.gaborbiro.dailymacros.features.modal.RecordsMapper
import dev.gaborbiro.dailymacros.features.modal.inputStreamToBase64
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.data.bitmap.ImageStore

internal class FoodPicSummaryUseCase(
    private val imageStore: ImageStore,
    private val chatGPTRepository: ChatGPTRepository,
    private val mapper: RecordsMapper,
) {

    suspend fun execute(filename: String): DialogState.InputDialog.SummarySuggestions {
        val response = try {
            val inputStream = imageStore.get(filename, thumbnail = false)
            chatGPTRepository.summarizeFoodPic(
                request = mapper.mapFoodPicsSummaryRequest(
                    base64Image = inputStreamToBase64(inputStream)
                )
            )
        } catch (apiError: ChatGPTApiError) {
            throw apiError
                .toDomainModel()
        }
        return mapper.map(response)
    }
}
