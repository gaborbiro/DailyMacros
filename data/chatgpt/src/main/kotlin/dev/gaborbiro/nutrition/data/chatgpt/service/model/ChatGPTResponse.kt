package dev.gaborbiro.nutrition.data.chatgpt.service.model

internal data class ChatGPTResponse(
    val choices: List<Choice>,
)

internal data class Choice(
    val message: Message,
)