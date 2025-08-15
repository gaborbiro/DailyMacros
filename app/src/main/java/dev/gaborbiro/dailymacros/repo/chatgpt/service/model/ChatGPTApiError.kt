package dev.gaborbiro.dailymacros.repo.chatgpt.service.model

internal sealed class ChatGPTApiError(
    override val message: String?,
    override val cause: Throwable? = null,
) : Exception(message) {

    data object ContentNotFoundError : ChatGPTApiError(null, null)

    data class GenericApiError(
        override val message: String?,
        override val cause: Throwable? = null,
    ) : ChatGPTApiError(message, cause)

    data class InternetApiError(override val cause: Throwable? = null) :
        ChatGPTApiError(message = null, cause = cause)

    data class AuthApiError(override val message: String?) : ChatGPTApiError(message)

    data class MappingApiError(
        override val message: String,
        override val cause: Throwable? = null,
    ) : ChatGPTApiError(message, cause)
}
