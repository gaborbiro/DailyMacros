package dev.gaborbiro.nutri.data.chatgpt

import dev.gaborbiro.nutri.data.chatgpt.model.NutrientsRequest
import dev.gaborbiro.nutri.data.chatgpt.model.NutrientsResponse
import dev.gaborbiro.nutri.data.chatgpt.model.FoodPicSummaryRequest
import dev.gaborbiro.nutri.data.chatgpt.model.FoodPicSummaryResponse


interface ChatGPTRepository {

    suspend fun summarizeFoodPic(request: FoodPicSummaryRequest): FoodPicSummaryResponse

    suspend fun nutrients(request: NutrientsRequest): NutrientsResponse
}
