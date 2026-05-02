package dev.gaborbiro.dailymacros.features.settings.export.useCases

import android.app.Application
import androidx.activity.ComponentActivity
import dev.gaborbiro.dailymacros.features.settings.export.OpenDocumentCancelled
import dev.gaborbiro.dailymacros.features.settings.export.OpenPublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.export.ProcessRestarter
import dev.gaborbiro.dailymacros.repositories.backup.domain.BackupRepository
import dev.gaborbiro.dailymacros.repositories.backup.domain.model.DatabaseBackupImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImportSqliteDatabaseUseCase(
    private val application: Application,
    private val backupRepository: BackupRepository,
    private val openPublicDocumentUseCase: OpenPublicDocumentUseCase,
    private val activityProvider: () -> ComponentActivity,
) {

    suspend fun execute(): ImportSqliteDatabaseResult {
        val uri = openPublicDocumentUseCase.execute().getOrElse { t ->
            return when (t) {
                is OpenDocumentCancelled -> ImportSqliteDatabaseResult.Cancelled
                else -> ImportSqliteDatabaseResult.Error(t.message ?: t.toString())
            }
        }

        val resolver = application.contentResolver

        return withContext(Dispatchers.IO) {
            val input = resolver.openInputStream(uri)
                ?: return@withContext ImportSqliteDatabaseResult.Error("Unable to read selected backup")

            input.use { input ->
                when (val result = backupRepository.importBackup(input)) {
                    DatabaseBackupImportResult.ReplacementApplied -> {
                        withContext(Dispatchers.Main) {
                            ProcessRestarter.restartApplication(activityProvider())
                        }
                        ImportSqliteDatabaseResult.RestartPending
                    }

                    DatabaseBackupImportResult.InvalidFile ->
                        ImportSqliteDatabaseResult.InvalidFile

                    is DatabaseBackupImportResult.IoFailure ->
                        ImportSqliteDatabaseResult.Error(result.message)
                }
            }
        }
    }
}

sealed class ImportSqliteDatabaseResult {
    data object Cancelled : ImportSqliteDatabaseResult()
    data object InvalidFile : ImportSqliteDatabaseResult()
    data object RestartPending : ImportSqliteDatabaseResult()
    data class Error(val message: String) : ImportSqliteDatabaseResult()
}
