package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.modal.RecordsMapper
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.data.image.ImageStore
import dev.gaborbiro.dailymacros.repo.chatgpt.ChatGPTRepository
import dev.gaborbiro.dailymacros.repo.labeler.OfflineLabelerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class FoodPicSummaryUseCase(
    private val imageStore: ImageStore,
    private val chatGPTRepository: ChatGPTRepository,
    private val labelerRepository: OfflineLabelerRepository,
    private val mapper: RecordsMapper,
) {

    suspend fun execute(filename: String): Flow<DialogState.InputDialog.SummarySuggestions> {
//        val response = try {
//            val inputStream = imageStore.get(filename, thumbnail = false)
//            chatGPTRepository.summarizeFoodPic(
//                request = mapper.mapFoodPicsSummaryRequest(
//                    base64Image = inputStreamToBase64(inputStream)
//                )
//            )
//        } catch (apiError: ChatGPTApiError) {
//            throw apiError
//                .toDomainModel()
//        }
//        return mapper.map(response)
        return labelerRepository
            .labelImage(filename)
            .map {
                DialogState.InputDialog.SummarySuggestions(
                    titles = it,
                    description = null,
                )
        }
    }
}
