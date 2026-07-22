package dev.gaborbiro.dailymacros.repositories.chatgpt.utils

import com.google.gson.Gson
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.AiRequestError
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ErrorResponseBody1
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ErrorResponseBody2
import dev.gaborbiro.dailymacros.repositories.common.model.UsageLimitKind
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

class ErrorHandlingContext(val tag: String)

/**
 * @throws AiRequestError
 */
internal suspend fun <T> runCatching(
    logTag: String,
    body: suspend ErrorHandlingContext.() -> T,
): T {
    return try {
        body(ErrorHandlingContext(logTag))
    } catch (e: AiRequestError) {
        throw e
    } catch (e: CancellationException) {
        // not mapping this into AiRequestError means upstreams won't pick up on it, allowing quiet task cancellations
        throw e
    } catch (e: IOException) {
        throw AiRequestError.Network(cause = e)
    } catch (t: Throwable) {
        throw AiRequestError.Generic(analyticsMessage = "$logTag Error: ${t.message}", cause = t)
    }
}

/**
 * Use this when there is a return value
 *
 * @param doOnError called before the default error handling mechanism. This means you can
 * override the default error handling by throwing your own ApiError.
 */
fun <T> ErrorHandlingContext.parse(
    response: Response<T>,
    doOnSuccess: ((T, Response<T>) -> Unit)? = null,
    doOnError: ((errorBody: ResponseBody?, response: Response<T>) -> Unit)? = null,
): T {
    handle(
        response,
        doOnSuccess = { response ->
            val body =
                response.body()
                    ?: throw AiRequestError.Generic("$tag Error: missing response payload")
            doOnSuccess?.invoke(body, response)
        },
        doOnError
    )
    return response.body()
        ?: throw AiRequestError.Generic("$tag Error: missing response payload")
}

/**
 * Use this when there is no return value
 *
 * @throws AiRequestError
 */
fun <T> ErrorHandlingContext.handle(
    response: Response<T>,
    doOnSuccess: ((Response<T>) -> Unit)? = null,
    doOnError: ((errorBody: ResponseBody?, response: Response<T>) -> Unit)? = null,
) {
    if (response.isSuccessful) {
        doOnSuccess?.invoke(response)
    } else {
        handleUnsuccessful(response, doOnError)
    }
}

/**
 * @throws AiRequestError
 */
private fun <T> ErrorHandlingContext.handleUnsuccessful(
    response: Response<T>,
    doOnError: ((errorBody: ResponseBody?, response: Response<T>) -> Unit)? = null,
): T {
    doOnError?.invoke(response.errorBody(), response)
    val errorBody = response.errorBody()?.string()
    val gson = Gson()

    // The proxy shape is {"error":{"message","type","code"}}; a bare {"message"}
    // is the fallback. Parse the structured error first so we can read `code`.
    val error = runCatching {
        gson.fromJson(errorBody, ErrorResponseBody1::class.java)?.error
    }.getOrNull()

    // A usage limit is enforced by the Firebase proxy, not the AI model, so it
    // gets its own error rather than the generic upstream one.
    proxyCodeToKind(error?.code)?.let { kind ->
        throw AiRequestError.Proxy(kind = kind)
    }

    val (analyticsMessage, type) = if (error != null) {
        error.message to error.type
    } else {
        val message = runCatching {
            gson.fromJson(errorBody, ErrorResponseBody2::class.java)?.message
        }.getOrNull()
        message to null
    }
    val finalType = type ?: "unknown error type"
    val finalMessage = analyticsMessage ?: "no error message"
    throw AiRequestError.Upstream(errorMessage = "$finalType - $finalMessage")
}

/**
 * Maps the Firebase proxy's usage-limit `code` (see `functions/index.js`) to a
 * [UsageLimitKind]; null for any other/absent code. This is the single
 * client-side location that knows the proxy's cap codes.
 */
private fun proxyCodeToKind(code: String?): UsageLimitKind? = when (code) {
    "daily_cap" -> UsageLimitKind.DAILY
    "monthly_budget" -> UsageLimitKind.MONTHLY
    "kill_switch" -> UsageLimitKind.UNAVAILABLE
    else -> null
}
