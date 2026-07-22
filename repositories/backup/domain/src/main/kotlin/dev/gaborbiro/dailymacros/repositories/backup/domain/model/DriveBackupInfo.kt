package dev.gaborbiro.dailymacros.repositories.backup.domain.model

data class DriveBackupInfo(
    val fileId: String,
    val modifiedTimeMs: Long,
    val sizeBytes: Long,
)
