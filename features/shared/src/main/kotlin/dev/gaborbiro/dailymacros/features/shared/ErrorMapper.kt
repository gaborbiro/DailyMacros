package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import javax.inject.Inject

class ErrorMapper @Inject constructor() {

    fun mapErrorMessage(error: DomainError): String = when (error) {
        is DomainError.DisplayMessageToUser.CheckInternetConnection -> "Internet connectivity error"
        is DomainError.DisplayMessageToUser.ContactSupport ->
            "Oops. Something went wrong. The issue has been logged and our engineers are looking into it."
        is DomainError.DisplayMessageToUser.Message -> error.message
        is DomainError.DisplayMessageToUser.TryAgain ->
            "Oops. Something went wrong. Please try again later."
    }
}
