package dev.gaborbiro.dailymacros.repositories.chatgpt.util

import com.google.gson.Gson
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ErrorResponseBody1
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ErrorResponseBody2
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

class ErrorHandlingContext(val tag: String)

/**
 * @throws ChatGPTApiError
 */
internal suspend fun <T> runCatching(
    logTag: String,
    body: suspend ErrorHandlingContext.() -> T,
): T {
    return try {
        body(ErrorHandlingContext(logTag))
    } catch (e: ChatGPTApiError) {
        throw e
    } catch (e: CancellationException) {
        // not mapping this into ChatGPTApiError means upstreams won't pick up on it, allowing quiet task cancellations
        throw e
    } catch (e: IOException) {
        throw ChatGPTApiError.InternetError(cause = e)
    } catch (t: Throwable) {
        throw ChatGPTApiError.GenericError(analyticsMessage = "$logTag Error: ${t.message}", cause = t)
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
                    ?: throw ChatGPTApiError.GenericError("$tag Error: missing response payload")
            doOnSuccess?.invoke(body, response)
        },
        doOnError
    )
    return response.body()
        ?: throw ChatGPTApiError.GenericError("$tag Error: missing response payload")
}

/**
 * Use this when there is no return value
 *
 * @throws ChatGPTApiError
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
 * @throws ChatGPTApiError
 */
private fun <T> ErrorHandlingContext.handleUnsuccessful(
    response: Response<T>,
    doOnError: ((errorBody: ResponseBody?, response: Response<T>) -> Unit)? = null,
): T {
    doOnError?.invoke(response.errorBody(), response)
    val errorBody = response.errorBody()?.string()
    val gson = Gson()

    val (analyticsMessage, type) = runCatching {
        gson.fromJson(
            errorBody,
            ErrorResponseBody1::class.java
        ).error
            ?.let { it.message to it.type }
            ?: run {
                val body = gson.fromJson(
                    errorBody,
                    ErrorResponseBody2::class.java
                )
                body.message to (null as String)
            }
    }.recover {
        runCatching {
            val body = gson.fromJson(
                errorBody,
                ErrorResponseBody2::class.java
            )
            body.message to null
        }.getOrDefault(null to null)
    }.getOrDefault(null to null)
    val finalType = type ?: "unknown type"
    val finalMessage = analyticsMessage ?: "no error message"
    throw ChatGPTApiError.GenericError(analyticsMessage = "$finalType - $finalMessage")
}
