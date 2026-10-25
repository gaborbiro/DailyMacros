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

    /**
     * Classifies a failure so the presentation layer can pick a message. Carries no text of its
     * own: the technical details live in [cause], so they're logged once, not repeated here.
     */
    sealed class DisplayMessageToUser(
        cause: Throwable?,
    ) : DomainError(message = null, cause) {

        data class OperationFailed(
            override val cause: Throwable? = null,
        ) : DisplayMessageToUser(cause)

        data class CheckInternetConnection(
            override val cause: Throwable,
        ) : DisplayMessageToUser(cause = cause)
    }
}
