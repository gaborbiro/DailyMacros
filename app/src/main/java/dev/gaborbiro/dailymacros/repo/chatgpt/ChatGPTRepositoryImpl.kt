package dev.gaborbiro.dailymacros.repo.chatgpt

import dev.gaborbiro.dailymacros.repo.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.PhotoAnalysisRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.PhotoAnalysisResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.repo.chatgpt.util.parse
import dev.gaborbiro.dailymacros.repo.chatgpt.util.runCatching


internal class ChatGPTRepositoryImpl(
    private val service: ChatGPTService,
) : ChatGPTRepository {

    override suspend fun analysePhotos(request: PhotoAnalysisRequest): PhotoAnalysisResponse {
        return runCatching(logTag = "analysePhotos") {
            val response = service.callResponses(
                request = request.toApiModel(),
            )
            return@runCatching parse(response)
                .toPhotoAnalysisResponse()
        }
    }

    override suspend fun getMacros(request: MacrosRequest): MacrosResponse {
        return runCatching(logTag = "getMacros") {
            val response = service.callResponses(
                request = request.toApiModel(),
            )
            return@runCatching parse(response)
                .toMacrosResponse()
        }
    }
}
