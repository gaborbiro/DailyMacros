package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import javax.inject.Inject

class ErrorUiMapper @Inject constructor() {

    fun mapErrorMessage(error: DomainError, defaultMessage: String): String = when (error) {
        is DomainError.DisplayMessageToUser.OperationFailed -> defaultMessage
        is DomainError.DisplayMessageToUser.CheckInternetConnection -> "Connectivity error. Please check your internet connection and try again."
        is DomainError.DisplayMessageToUser.ForceTechnicalMessage -> error.errorMessage
    }
}
