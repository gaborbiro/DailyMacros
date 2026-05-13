package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTApiError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatGPTMapper @Inject constructor() {

    fun map(error: ChatGPTApiError): ChatGPTDomainError {
        return when (error) {
            is ChatGPTApiError.AuthApiError -> ChatGPTDomainError.DisplayMessageToUser.Message("Error talking to AI")
            is ChatGPTApiError.InternetApiError -> ChatGPTDomainError.DisplayMessageToUser.CheckInternetConnection(error)
            is ChatGPTApiError.MappingApiError, is ChatGPTApiError.ContentNotFoundError -> ChatGPTDomainError.DisplayMessageToUser.ContactSupport(error)
            is ChatGPTApiError.GenericApiError -> error.message
                ?.let { ChatGPTDomainError.DisplayMessageToUser.Message(it, error) }
                ?: ChatGPTDomainError.DisplayMessageToUser.TryAgain(error)
        }
    }
}