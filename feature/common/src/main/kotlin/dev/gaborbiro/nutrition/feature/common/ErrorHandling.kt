package dev.gaborbiro.nutrition.feature.common

import dev.gaborbiro.nutrition.data.common.model.DomainError
import dev.gaborbiro.nutrition.core.clause.*


fun DomainError.errorMessage(): Clause.Text {
    return when (this) {
        is DomainError.DisplayMessageToUser -> {
            when (this) {
                is DomainError.DisplayMessageToUser.Message -> Clause.Text.Plain(
                    message
                )
                DomainError.DisplayMessageToUser.CheckInternetConnection -> Clause.Text.Localised(
                    R.string.error_connectivity
                )
                DomainError.DisplayMessageToUser.ContactSupport -> Clause.Text.Localised(
                    R.string.error_contact_support
                )
                DomainError.DisplayMessageToUser.TryAgain -> Clause.Text.Localised(
                    R.string.error_try_again
                )
            }
        }

        is DomainError.GoToSignInScreen -> {
            message?.asText()
                ?: Clause.Text.Localised(R.string.error_session_expired_sign_in)
        }
    }
}