package dev.gaborbiro.nutrition.data.chatgpt

import dev.gaborbiro.nutrition.data.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.nutrition.data.chatgpt.domain.model.Request
import dev.gaborbiro.nutrition.data.chatgpt.domain.model.Response
import dev.gaborbiro.nutrition.data.chatgpt.service.ChatGPTService
import dev.gaborbiro.nutrition.data.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.nutrition.data.chatgpt.util.parse
import dev.gaborbiro.nutrition.data.chatgpt.util.runCatching
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class ChatGPTRepositoryImpl @Inject constructor(
    private val service: ChatGPTService,
) : ChatGPTRepository {

    override suspend fun request(request: Request): Response {
        try {
            return runCatching(logTag = "getChatCompletions") {
                val response = service.getChatCompletions(
                    request = request.toApiModel(),
                )
                return@runCatching parse(response)
                    .toDomainModel()
            }
        } catch (apiError: ChatGPTApiError) {
            throw apiError
                .toDomainModel()
        }
    }
}