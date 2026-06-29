package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import javax.inject.Inject

class ErrorUiMapper @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    fun mapErrorMessage(error: DomainError, defaultMessage: String): String = when (error) {
        is DomainError.DisplayMessageToUser.OperationFailed -> defaultMessage
        is DomainError.DisplayMessageToUser.CheckInternetConnection ->
            "Connectivity error. Please check your internet connection and try again."
        is DomainError.DisplayMessageToUser.TechnicalMessage ->
            if (settingsRepository.getApiKeyOverride() != null) error.errorMessage else defaultMessage
        is DomainError.DisplayMessageToUser.ForceTechnicalMessage -> error.errorMessage
    }
}
