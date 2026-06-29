package dev.gaborbiro.dailymacros.repositories.common.model

sealed class DomainError(
    analyticsMessage: String?,
    cause: Throwable? = null,
) : Exception(analyticsMessage, cause) {

    sealed class DisplayMessageToUser(
        analyticsMessage: String? = null,
        open val errorMessage: String? = null,
        cause: Throwable?,
    ) : DomainError(analyticsMessage, cause) {

        data class ForceTechnicalMessage(
            private val analyticsMessage: String?,
            override val errorMessage: String,
            override val cause: Throwable? = null,
        ) : DisplayMessageToUser(analyticsMessage, errorMessage, cause)

        data class OperationFailed(
            private val analyticsMessage: String? = null,
            override val cause: Throwable? = null,
        ) : DisplayMessageToUser(analyticsMessage = analyticsMessage, cause = cause)

        data class CheckInternetConnection(
            override val cause: Throwable,
        ) : DisplayMessageToUser(cause = cause)
    }
}
