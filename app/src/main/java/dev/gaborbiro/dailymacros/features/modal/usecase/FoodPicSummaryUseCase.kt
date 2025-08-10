package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.data.chatgpt.ChatGPTRepository
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.data.chatgpt.toDomainModel
import dev.gaborbiro.dailymacros.features.modal.RecordsMapper
import dev.gaborbiro.dailymacros.features.common.inputStreamToBase64
import dev.gaborbiro.dailymacros.store.bitmap.BitmapStore

class FoodPicSummaryUseCase(
    private val bitmapStore: BitmapStore,
    private val chatGPTRepository: ChatGPTRepository,
    private val mapper: RecordsMapper,
) {

    suspend fun execute(filename: String): List<String> {
        val response = try {
            val inputStream = bitmapStore.get(filename, thumbnail = false)
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
