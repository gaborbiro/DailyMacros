package dev.gaborbiro.dailymacros.core.featureflags

/**
 * Supplies the stable, per-install three-word id, attached to Remote Config as
 * a custom signal so a flag can be targeted at one specific install (e.g. keep
 * a feature hidden from everyone except a test/support device) without wiring
 * up a Firebase Analytics audience.
 *
 * Implemented in the app module over the same source as the chatgpt proxy's
 * ClientIdProvider, so the id a user reads off the Settings screen is always
 * the one Remote Config conditions are matched against.
 */
interface RemoteConfigClientIdProvider {
    val clientId: String
}
