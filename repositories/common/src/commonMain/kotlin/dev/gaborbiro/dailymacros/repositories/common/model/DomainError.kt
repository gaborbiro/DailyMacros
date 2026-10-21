package dev.gaborbiro.dailymacros.repositories.common.model

sealed class DomainError(
    message: String?,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * Keeps the platform's `ClassName: message` form. Being `final`, it stops the data class
     * subtypes from generating a `toString()` that embeds the whole cause chain, which
     * stack traces would then repeat on every `Caused by:` line.
     */
    final override fun toString(): String = super.toString()

    sealed class DisplayMessageToUser(
        message: String? = null,
        cause: Throwable?,
    ) : DomainError(message, cause) {

        data class TechnicalMessage(
            override val message: String,
            override val cause: Throwable? = null,
        ) : DisplayMessageToUser(message, cause)

        data class ForceTechnicalMessage(
            override val message: String,
            override val cause: Throwable? = null,
        ) : DisplayMessageToUser(message, cause)

        data class OperationFailed(
            override val message: String? = null,
            override val cause: Throwable? = null,
        ) : DisplayMessageToUser(message, cause = cause)

        data class CheckInternetConnection(
            override val cause: Throwable,
        ) : DisplayMessageToUser(cause = cause)
    }
}
