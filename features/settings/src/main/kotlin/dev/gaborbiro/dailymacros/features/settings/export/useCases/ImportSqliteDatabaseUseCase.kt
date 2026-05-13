package dev.gaborbiro.dailymacros.features.settings.export.useCases

import android.app.Application
import android.util.Log
import dev.gaborbiro.dailymacros.features.settings.export.OpenDocumentCancelled
import dev.gaborbiro.dailymacros.features.settings.export.OpenPublicDocumentUseCase
import dev.gaborbiro.dailymacros.repositories.backup.domain.BackupRepository
import dev.gaborbiro.dailymacros.repositories.backup.domain.model.DatabaseBackupImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImportSqliteDatabaseUseCase @Inject constructor(
    private val application: Application,
    private val backupRepository: BackupRepository,
) {

    suspend fun execute(openPublicDocumentUseCase: OpenPublicDocumentUseCase): ImportSqliteDatabaseResult {
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
                    DatabaseBackupImportResult.ReplacementApplied ->
                        ImportSqliteDatabaseResult.RestartPending

                    DatabaseBackupImportResult.InvalidFile ->
                        ImportSqliteDatabaseResult.InvalidFile

                    is DatabaseBackupImportResult.IoFailure -> {
                        // The DB is already closed at this point; on-disk state was rolled back
                        // but the in-memory singleton is unusable. We must restart regardless.
                        Log.w(TAG, "Database swap failed; restart required: ${result.message}")
                        ImportSqliteDatabaseResult.RestartPending
                    }
                }
            }
        }
    }

    private companion object {
        private const val TAG = "ImportSqliteDb"
    }
}

sealed class ImportSqliteDatabaseResult {
    data object Cancelled : ImportSqliteDatabaseResult()
    data object InvalidFile : ImportSqliteDatabaseResult()
    data object RestartPending : ImportSqliteDatabaseResult()
    data class Error(val message: String) : ImportSqliteDatabaseResult()
}

