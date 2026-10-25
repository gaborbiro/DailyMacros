package dev.gaborbiro.dailymacros.features.shared

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import dev.gaborbiro.dailymacros.repositories.common.model.UsageLimitException
import dev.gaborbiro.dailymacros.repositories.common.model.UsageLimitKind
import javax.inject.Inject

class ErrorUiMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Picks the message the user is shown for [error], falling back to [genericMessage]. The
     * result is what to both display and log.
     */
    fun map(error: Throwable, genericMessage: String): UserFacingError =
        UserFacingError(message = mapMessage(error, genericMessage), cause = error)

    private fun mapMessage(error: Throwable, genericMessage: String): String = when (error) {
        is DomainError.DisplayMessageToUser.OperationFailed -> when (val cause = error.cause) {
            // A usage limit shows a specific message; anything else stays generic.
            is UsageLimitException -> context.getString(
                when (cause.kind) {
                    UsageLimitKind.DAILY -> R.string.shared_content_usage_limit_daily
                    UsageLimitKind.MONTHLY -> R.string.shared_content_usage_limit_monthly
                    UsageLimitKind.UNAVAILABLE -> R.string.shared_content_usage_limit_unavailable
                    UsageLimitKind.SUBSCRIPTION_REQUIRED -> R.string.shared_content_usage_limit_subscription_required
                }
            )
            else -> genericMessage
        }
        is DomainError.DisplayMessageToUser.CheckInternetConnection ->
            context.getString(R.string.shared_content_connectivity_error)
        else -> genericMessage
    }
}
