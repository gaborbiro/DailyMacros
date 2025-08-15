package dev.gaborbiro.dailymacros.repo.chatgpt

import dev.gaborbiro.dailymacros.repo.chatgpt.model.DomainError
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTApiError


internal fun ChatGPTApiError.toDomainModel(): DomainError {
    return when (this) {
        is ChatGPTApiError.AuthApiError -> DomainError.GoToSignInScreen(message, this)
        is ChatGPTApiError.InternetApiError -> DomainError.DisplayMessageToUser.CheckInternetConnection(this)
        is ChatGPTApiError.MappingApiError, is ChatGPTApiError.ContentNotFoundError -> DomainError.DisplayMessageToUser.ContactSupport(this)
        is ChatGPTApiError.GenericApiError -> message
            ?.let { DomainError.DisplayMessageToUser.Message(it, this) }
            ?: DomainError.DisplayMessageToUser.TryAgain(this)
    }
}
