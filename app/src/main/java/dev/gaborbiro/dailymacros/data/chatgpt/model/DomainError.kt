package dev.gaborbiro.dailymacros.data.chatgpt.model


sealed class DomainError(message: String?, cause: Throwable? = null) : Exception(message, cause) {

    sealed class DisplayMessageToUser(
        override val message: String? = null,
        cause: Throwable? = null,
    ) : DomainError(message, cause) {
        data class Message(override val message: String, override val cause: Throwable? = null) :
            DisplayMessageToUser(message, cause)

        data class TryAgain(override val cause: Throwable? = null) :
            DisplayMessageToUser(message = null, cause)

        data class ContactSupport(override val cause: Throwable? = null) :
            DisplayMessageToUser(message = null, cause)

        data class CheckInternetConnection(override val cause: Throwable? = null) :
            DisplayMessageToUser(message = null, cause)
    }

    data class GoToSignInScreen(
        override val message: String? = null,
        override val cause: Throwable? = null,
    ) : DomainError(message, cause)
}
