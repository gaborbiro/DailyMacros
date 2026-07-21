package dev.gaborbiro.dailymacros.repositories.chatgpt.service.model

import dev.gaborbiro.dailymacros.repositories.common.model.UsageLimitKind

/**
 * An error raised while fulfilling an AI analysis request, grouped by origin.
 *
 * The request can fail at three distinct layers, which this type keeps separate:
 * transport ([Network]), the Firebase proxy that enforces usage limits ([Proxy]),
 * and the upstream AI model / OpenAI response ([Upstream]); plus client-side
 * [Generic] / [Mapping] failures. (Formerly `ChatGPTApiError`, which predated the
 * proxy layer and mislabelled proxy-origin errors as coming from ChatGPT.)
 */
sealed class AiRequestError(
    open val analyticsMessage: String?,
    override val cause: Throwable? = null,
) : Exception(analyticsMessage) {

    /** No connectivity / transport failure. */
    data class Network(override val cause: Throwable? = null) :
        AiRequestError(analyticsMessage = null, cause = cause)

    /** The Firebase proxy rejected the request because a usage limit was hit. */
    data class Proxy(
        val kind: UsageLimitKind,
        override val cause: Throwable? = null,
    ) : AiRequestError(analyticsMessage = "proxy: $kind", cause)

    /** A non-2xx response from the AI model / OpenAI (or an unclassified upstream error). */
    data class Upstream(
        val errorMessage: String,
        override val cause: Throwable? = null,
    ) : AiRequestError(analyticsMessage = errorMessage, cause)

    /** Anything else that went wrong while making the call. */
    data class Generic(
        override val analyticsMessage: String?,
        override val cause: Throwable? = null,
    ) : AiRequestError(analyticsMessage, cause)

    /** A response was received but could not be mapped to the expected shape. */
    data class Mapping(
        override val analyticsMessage: String,
        override val cause: Throwable? = null,
    ) : AiRequestError(analyticsMessage, cause)
}
