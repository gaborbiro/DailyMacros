package dev.gaborbiro.dailymacros.features.settings.export.useCases

import dev.gaborbiro.dailymacros.features.settings.export.CreatePublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.export.StreamWriter
import dev.gaborbiro.dailymacros.repositories.backup.domain.BackupRepository
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ExportSqliteDatabaseUseCase(
    private val backupRepository: BackupRepository,
    private val createPublicDocumentUseCase: CreatePublicDocumentUseCase,
    private val streamWriter: StreamWriter,
//    private val sharePublicUriLauncher: SharePublicUriLauncher,
) {

    suspend fun execute() {
        val archiveFile = backupRepository.prepareBackupArchiveForExport()
        try {
            val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val fileName = "daily_macros_backup_$stamp.tar"

            val uri = createPublicDocumentUseCase.execute(fileName)
                ?: return

            streamWriter.execute(uri) { output ->
                FileInputStream(archiveFile).use { input ->
                    input.copyTo(output)
                }
                output.flush()
            }

//            sharePublicUriLauncher.execute(
//                uri,
//                mimeType = "application/x-tar",
//                chooserTitle = "Share backup",
//            )
        } finally {
            archiveFile.delete()
        }
    }
}
