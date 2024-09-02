package dev.gaborbiro.nutrition.data.chatgpt.domain

import dev.gaborbiro.nutrition.data.chatgpt.domain.model.Request
import dev.gaborbiro.nutrition.data.chatgpt.domain.model.Response


interface ChatGPTRepository {

    suspend fun request(request: Request): Response
}
