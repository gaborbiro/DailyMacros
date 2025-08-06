package dev.gaborbiro.nutri.features.modal.usecase

import dev.gaborbiro.nutri.data.chatgpt.ChatGPTRepository
import dev.gaborbiro.nutri.data.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.nutri.data.chatgpt.toDomainModel
import dev.gaborbiro.nutri.features.common.RecordsMapper
import dev.gaborbiro.nutri.features.common.inputStreamToBase64
import dev.gaborbiro.nutri.store.bitmap.BitmapStore

class FoodPicSummaryUseCase(
    private val bitmapStore: BitmapStore,
    private val chatGPTRepository: ChatGPTRepository,
    private val mapper: RecordsMapper,
) {

    suspend fun execute(filename: String): String? {
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
