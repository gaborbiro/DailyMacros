package dev.gaborbiro.dailymacros.features.shared

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import javax.inject.Inject

class ErrorUiMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {

    fun mapErrorMessage(error: DomainError, defaultMessage: String): String = when (error) {
        is DomainError.DisplayMessageToUser.OperationFailed -> defaultMessage
        is DomainError.DisplayMessageToUser.CheckInternetConnection ->
            context.getString(R.string.shared_content_connectivity_error)
        is DomainError.DisplayMessageToUser.TechnicalMessage ->
            if (settingsRepository.getApiKeyOverride() != null) error.errorMessage else defaultMessage
        is DomainError.DisplayMessageToUser.ForceTechnicalMessage -> error.errorMessage
    }
}
