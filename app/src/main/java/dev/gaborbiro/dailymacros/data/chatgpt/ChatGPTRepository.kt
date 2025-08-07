package dev.gaborbiro.dailymacros.data.chatgpt

import dev.gaborbiro.dailymacros.data.chatgpt.model.NutrientsRequest
import dev.gaborbiro.dailymacros.data.chatgpt.model.NutrientsResponse
import dev.gaborbiro.dailymacros.data.chatgpt.model.FoodPicSummaryRequest
import dev.gaborbiro.dailymacros.data.chatgpt.model.FoodPicSummaryResponse


interface ChatGPTRepository {

    suspend fun summarizeFoodPic(request: FoodPicSummaryRequest): FoodPicSummaryResponse

    suspend fun nutrients(request: NutrientsRequest): NutrientsResponse
}
