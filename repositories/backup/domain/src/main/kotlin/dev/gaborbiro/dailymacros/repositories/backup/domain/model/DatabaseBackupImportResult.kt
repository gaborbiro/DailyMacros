package dev.gaborbiro.dailymacros.repositories.backup.domain.model

sealed class DatabaseBackupImportResult {
    /** Replacement committed successfully; restart the app process next. */
    data object ReplacementApplied : DatabaseBackupImportResult()

    data object InvalidFile : DatabaseBackupImportResult()

    data class IoFailure(val message: String) : DatabaseBackupImportResult()
}