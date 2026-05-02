package dev.gaborbiro.dailymacros.repositories.backup.domain

import dev.gaborbiro.dailymacros.repositories.backup.domain.model.DatabaseBackupImportResult
import java.io.File
import java.io.InputStream

interface BackupRepository {

    /**
     * WAL-checkpoints the DB, then writes a POSIX tar to a new temp file containing:
     * - [backup_manifest.json] (format marker)
     * - [databases/daily_macros_db]
     * - [files/public/...] image files (if any)
     *
     * Caller must delete the returned file when finished.
     * @throws IllegalStateException when the database file does not exist yet.
     */
    suspend fun prepareBackupArchiveForExport(): File

    /** Restores from a full tar created by [prepareBackupArchiveForExport]. */
    suspend fun importBackup(source: InputStream): DatabaseBackupImportResult
}
