package dev.gaborbiro.dailymacros.repo.chatgpt

import dev.gaborbiro.dailymacros.repo.chatgpt.model.FoodPicSummaryRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.model.FoodPicSummaryResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.model.NutrientsRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.model.NutrientsResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repo.chatgpt.util.parse
import dev.gaborbiro.dailymacros.repo.chatgpt.util.runCatching


internal class ChatGPTRepositoryImpl(
    private val service: ChatGPTService,
) : ChatGPTRepository {

    override suspend fun summarizeFoodPic(request: FoodPicSummaryRequest): FoodPicSummaryResponse {
        try {
            return runCatching(logTag = "summarizeFoodPic") {
                val response = service.callResponses(
                    request = request.toApiModel(),
                )
                return@runCatching parse(response)
                    .toFoodPicSummaryResponse()
            }
        } catch (apiError: ChatGPTApiError) {
            throw apiError
                .toDomainModel()
        }
    }

    override suspend fun nutrients(request: NutrientsRequest): NutrientsResponse {
        try {
            return runCatching(logTag = "nutrients") {
                val response = service.callResponses(
                    request = request.toApiModel(),
                )
                return@runCatching parse(response)
                    .toNutrientsResponse()
            }
        } catch (apiError: ChatGPTApiError) {
            throw apiError
                .toDomainModel()
        }
    }
}
