package dev.gaborbiro.dailymacros.repositories.backup.domain.model

data class DriveBackupInfo(
    val fileId: String,
    val modifiedTimeMs: Long,
    val sizeBytes: Long,
    /**
     * True if this install made the latest write to the backup. False for backups written by
     * another install, and for ones uploaded before writers were tagged.
     */
    val writtenByThisInstall: Boolean = false,
)
