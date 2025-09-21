package dev.gaborbiro.dailymacros.repo.chatgpt.domain

import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodPicSummaryRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodPicSummaryResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosResponse

interface ChatGPTRepository {

    suspend fun summarizeFoodPic(request: FoodPicSummaryRequest): FoodPicSummaryResponse

    suspend fun macros(request: MacrosRequest): MacrosResponse
}
