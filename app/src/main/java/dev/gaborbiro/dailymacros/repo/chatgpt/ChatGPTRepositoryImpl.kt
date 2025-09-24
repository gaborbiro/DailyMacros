package dev.gaborbiro.dailymacros.repo.chatgpt

import dev.gaborbiro.dailymacros.repo.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodPicSummaryRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodPicSummaryResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.repo.chatgpt.util.parse
import dev.gaborbiro.dailymacros.repo.chatgpt.util.runCatching


internal class ChatGPTRepositoryImpl(
    private val service: ChatGPTService,
) : ChatGPTRepository {

    override suspend fun summarizeFoodPic(request: FoodPicSummaryRequest): FoodPicSummaryResponse {
        return runCatching(logTag = "summarizeFoodPic") {
            val response = service.callResponses(
                request = request.toApiModel(),
            )
            return@runCatching parse(response)
                .toFoodPicSummaryResponse()
        }
    }

    override suspend fun macros(request: MacrosRequest): MacrosResponse {
        return runCatching(logTag = "macros") {
            val response = service.callResponses(
                request = request.toApiModel(),
            )
            return@runCatching parse(response)
                .toMacrosResponse()
        }
    }
}
