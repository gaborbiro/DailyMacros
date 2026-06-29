package dev.gaborbiro.dailymacros.repositories.chatgpt.utils

import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTApiError

fun <T> T?.guardNotNull(message: String): T {
    val value = this
    if (value == null) {
        throw ChatGPTApiError.MappingError(message)
    } else {
        return value
    }
}