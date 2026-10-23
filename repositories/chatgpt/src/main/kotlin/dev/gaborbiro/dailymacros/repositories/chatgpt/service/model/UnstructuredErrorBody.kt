package dev.gaborbiro.dailymacros.repositories.chatgpt.service.model

/**
 * A non-2xx response body with no recognisable error structure, kept verbatim as the cause of
 * an [AiRequestError.Upstream] so the raw server output reaches the logs. Deliberately outside
 * the [AiRequestError] hierarchy: it's evidence, not a failure category.
 */
class UnstructuredErrorBody(code: Int, body: String) : Exception("$code - $body")
