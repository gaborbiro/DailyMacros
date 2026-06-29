package dev.gaborbiro.dailymacros.repositories.chatgpt.service.model

sealed class ChatGPTApiError(
    open val analyticsMessage: String?,
    override val cause: Throwable? = null,
) : Exception(analyticsMessage) {

    data class GenericError(
        override val analyticsMessage: String?,
        override val cause: Throwable? = null,
    ) : ChatGPTApiError(analyticsMessage, cause)

    data class ServerErrorResponse(
        val errorMessage: String,
        override val cause: Throwable? = null,
    ) : ChatGPTApiError(analyticsMessage = errorMessage, cause)

    data class InternetError(override val cause: Throwable? = null) :
        ChatGPTApiError(analyticsMessage = null, cause = cause)

    data class MappingError(
        override val analyticsMessage: String,
        override val cause: Throwable? = null,
    ) : ChatGPTApiError(analyticsMessage, cause)
}
