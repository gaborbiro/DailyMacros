package dev.gaborbiro.dailymacros.repositories.backup.domain.model

sealed class DatabaseBackupImportResult {
    /** Replacement committed successfully; restart the app process next. */
    data object ReplacementApplied : DatabaseBackupImportResult()

    /** Source file is not a valid backup. Pre-swap failure; database untouched. */
    data object InvalidFile : DatabaseBackupImportResult()

    /**
     * Swap was attempted and failed. The on-disk database may have been rolled back, but the
     * in-memory [dev.gaborbiro.dailymacros.data.db.AppDatabase] singleton has been closed and is
     * unusable for the rest of this process. The caller MUST restart the app.
     */
    data class IoFailure(val message: String) : DatabaseBackupImportResult()
}
