package dev.gaborbiro.dailymacros.repositories.chatgpt.utils

import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.AiRequestError

fun <T> T?.guardNotNull(message: String): T {
    val value = this
    if (value == null) {
        throw AiRequestError.Mapping(message)
    } else {
        return value
    }
}