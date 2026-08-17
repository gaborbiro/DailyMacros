package dev.gaborbiro.dailymacros.repositories.settings.domain

/**
 * Stages "last synced" bookkeeping across a cloud-restore-triggered process restart.
 *
 * Restoring a cloud backup replaces `shared_prefs/` on disk directly, bypassing
 * SettingsRepository's already-cached SharedPreferences instance. Writing through that
 * stale instance right after such a restore would flush its whole in-memory map back
 * to disk, clobbering the just-restored file (see RestoreFromDriveUseCase). This store
 * persists the value outside `shared_prefs`, untouched by backup/restore, so it can be
 * applied via SettingsRepository once the restart has reloaded prefs fresh from disk.
 */
interface PendingDriveSyncInfoStore {
    fun write(driveModifiedAtMs: Long)

    /** Returns and clears the staged value, if any. */
    fun consumeIfPresent(): Long?
}
