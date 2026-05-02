package dev.gaborbiro.dailymacros.repositories.backup.domain

import dev.gaborbiro.dailymacros.repositories.backup.domain.model.DatabaseBackupImportResult
import java.io.File
import java.io.InputStream

interface BackupRepository {

    /**
     * WAL checkpoint into the main DB file and returns that file.
     * @throws IllegalStateException when the database file does not exist yet.
     */
    suspend fun prepareSqliteFileForExport(): File

    suspend fun importSqliteReplacement(source: InputStream): DatabaseBackupImportResult
}
