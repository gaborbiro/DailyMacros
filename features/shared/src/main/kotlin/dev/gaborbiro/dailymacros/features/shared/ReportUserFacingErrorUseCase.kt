package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import javax.inject.Inject

/**
 * Picks the message the user is shown for an error, logs it as a [UserFacingError] and returns the
 * message. Showing it (snackbar, toast, ...) stays with the caller.
 */
class ReportUserFacingErrorUseCase @Inject constructor(
    private val errorUiMapper: ErrorUiMapper,
    private val analyticsLogger: AnalyticsLogger,
) {

    /** [genericMessage] is shown when [error] has no more specific message of its own. */
    fun execute(error: Throwable, genericMessage: String): String {
        val userFacingError = errorUiMapper.map(error, genericMessage)
        analyticsLogger.logError(userFacingError)
        return userFacingError.message
    }
}
