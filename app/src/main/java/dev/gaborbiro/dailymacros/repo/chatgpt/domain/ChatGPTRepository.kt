package dev.gaborbiro.dailymacros.repo.chatgpt.domain

import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.PhotoAnalysisRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.PhotoAnalysisResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosResponse

interface ChatGPTRepository {

    suspend fun analysePhotos(request: PhotoAnalysisRequest): PhotoAnalysisResponse

    suspend fun getMacros(request: MacrosRequest): MacrosResponse
}
