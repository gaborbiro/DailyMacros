package dev.gaborbiro.dailymacros.repositories.common.model

/**
 * Which server-side usage limit an AI request ran into. These originate at the
 * Firebase proxy layer (not the AI model): a per-user daily cap, the global
 * monthly budget, or the manual kill switch.
 */
enum class UsageLimitKind { DAILY, MONTHLY, UNAVAILABLE }

/**
 * Carried as the [cause] of a [DomainError.DisplayMessageToUser.OperationFailed]
 * so the UI layer can show a limit-specific message without a dedicated
 * DomainError subtype. Lives in the common module so both the repository (which
 * constructs it) and the feature layer (which reads it) can see it.
 */
class UsageLimitException(
    val kind: UsageLimitKind,
    cause: Throwable? = null,
) : Exception(cause)
