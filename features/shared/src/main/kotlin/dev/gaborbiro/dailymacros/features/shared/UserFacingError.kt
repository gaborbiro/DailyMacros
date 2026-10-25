package dev.gaborbiro.dailymacros.features.shared

/**
 * Top of a logged error chain: [message] is exactly what the user was shown and [cause] is the
 * error behind it. Log this rather than the underlying error, so Logcat and Crashlytics lead with
 * what the user saw.
 */
class UserFacingError(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)
