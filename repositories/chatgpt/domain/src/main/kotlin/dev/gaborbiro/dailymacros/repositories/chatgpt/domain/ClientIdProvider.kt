package dev.gaborbiro.dailymacros.repositories.chatgpt.domain

/**
 * Supplies the stable, user-facing client identifier — the three-word ID shown
 * on the Settings screen — so outbound proxy requests can be correlated to a
 * specific install when a user reports a problem.
 *
 * Implemented in the app module over the same source that renders the Settings
 * value, so the two never drift. This is intentionally NOT the Firebase auth
 * UID: the auth UID keys the usage document server-side, while this id is what
 * the user can actually read back to you in a support email.
 */
interface ClientIdProvider {
    val clientId: String
}
