package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError
import javax.inject.Inject

class MacrosNotificationTextMapper @Inject constructor() {

    fun mapDomainErrorToUserMessage(error: ChatGPTDomainError): String = when (error) {
        is ChatGPTDomainError.DisplayMessageToUser.CheckInternetConnection -> "Internet connectivity error"
        is ChatGPTDomainError.DisplayMessageToUser.ContactSupport ->
            "Oops. Something went wrong. The issue has been logged and our engineers are looking into it."
        is ChatGPTDomainError.DisplayMessageToUser.Message -> error.message
        is ChatGPTDomainError.DisplayMessageToUser.TryAgain ->
            "Oops. Something went wrong. Please try again later."
    }
}
