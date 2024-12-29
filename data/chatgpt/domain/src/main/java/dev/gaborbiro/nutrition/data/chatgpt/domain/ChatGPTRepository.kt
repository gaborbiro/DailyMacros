package dev.gaborbiro.nutrition.data.chatgpt.domain

import dev.gaborbiro.nutrition.data.chatgpt.domain.model.QueryRequest
import dev.gaborbiro.nutrition.data.chatgpt.domain.model.Response


interface ChatGPTRepository {

    suspend fun query(query: QueryRequest): Response
}
