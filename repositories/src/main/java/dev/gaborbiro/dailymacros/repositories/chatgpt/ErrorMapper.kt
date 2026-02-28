package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.DomainError
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTApiError


fun ChatGPTApiError.toDomainModel(): DomainError {
    return when (this) {
        is ChatGPTApiError.AuthApiError -> DomainError.DisplayMessageToUser.Message("Error talking to AI")
        is ChatGPTApiError.InternetApiError -> DomainError.DisplayMessageToUser.CheckInternetConnection(this)
        is ChatGPTApiError.MappingApiError, is ChatGPTApiError.ContentNotFoundError -> DomainError.DisplayMessageToUser.ContactSupport(this)
        is ChatGPTApiError.GenericApiError -> message
            ?.let { DomainError.DisplayMessageToUser.Message(it, this) }
            ?: DomainError.DisplayMessageToUser.TryAgain(this)
    }
}
