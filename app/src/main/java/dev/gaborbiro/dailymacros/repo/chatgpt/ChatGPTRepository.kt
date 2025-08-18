package dev.gaborbiro.dailymacros.repo.chatgpt

import dev.gaborbiro.dailymacros.repo.chatgpt.model.MacrosRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.model.MacrosResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.model.FoodPicSummaryRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.model.FoodPicSummaryResponse


interface ChatGPTRepository {

    suspend fun summarizeFoodPic(request: FoodPicSummaryRequest): FoodPicSummaryResponse

    suspend fun macros(request: MacrosRequest): MacrosResponse
}
