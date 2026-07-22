package dev.gaborbiro.dailymacros.repositories.chatgpt.prompts

import dev.gaborbiro.dailymacros.repositories.chatgpt.utils.guardNotNull
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.Role

fun ChatGPTResponse.resultJson(): String {
    return this.output
        .lastOrNull { it.role == Role.assistant && it.content?.any { it is OutputContent.Text } == true }.guardNotNull("Missing assistant content in ChatGPTResponse")
        .content.guardNotNull("Missing content in ChatGPTResponse")
        .filterIsInstance<OutputContent.Text>()
        .firstOrNull { it.text.isNotBlank() }.guardNotNull("Missing text entry in ChatGPTResponse")
        .text
}