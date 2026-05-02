package dev.gaborbiro.dailymacros.features.settings.export.useCases

import dev.gaborbiro.dailymacros.features.settings.export.CreatePublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.export.SharePublicUriLauncher
import dev.gaborbiro.dailymacros.features.settings.export.StreamWriter
import dev.gaborbiro.dailymacros.repositories.backup.domain.BackupRepository
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ExportSqliteDatabaseUseCase(
    private val backupRepository: BackupRepository,
    private val createPublicDocumentUseCase: CreatePublicDocumentUseCase,
    private val streamWriter: StreamWriter,
    private val sharePublicUriLauncher: SharePublicUriLauncher,
) {

    suspend fun execute() {
        val dbFile = backupRepository.prepareSqliteFileForExport()

        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val fileName = "daily_macros_backup_$stamp.db"

        val uri = createPublicDocumentUseCase.execute(fileName)
            ?: return

        streamWriter.execute(uri) { output ->
            FileInputStream(dbFile).use { input ->
                input.copyTo(output)
            }
            output.flush()
        }

        sharePublicUriLauncher.execute(
            uri,
            mimeType = "application/octet-stream",
            chooserTitle = "Share database backup",
        )
    }
}
