package dev.gaborbiro.dailymacros.repositories.settings.domain.model

enum class AutoSyncError {
    FAILURE,
    CONFLICT,
}

/**
 * The auto-sync error the user was last notified about. Present only while the error
 * episode is unresolved; cleared on any successful sync/restore or on sign-out.
 */
data class AutoSyncErrorStatus(
    val error: AutoSyncError,
    val notifiedEpochMs: Long,
)
