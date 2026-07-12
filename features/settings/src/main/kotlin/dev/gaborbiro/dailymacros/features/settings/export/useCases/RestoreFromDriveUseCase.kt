package dev.gaborbiro.dailymacros.features.settings.export.useCases

import dev.gaborbiro.dailymacros.repositories.backup.domain.BackupRepository
import dev.gaborbiro.dailymacros.repositories.backup.domain.CloudSyncRepository
import dev.gaborbiro.dailymacros.repositories.backup.domain.model.DatabaseBackupImportResult
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import javax.inject.Inject

class RestoreFromDriveUseCase @Inject constructor(
    private val cloudSyncRepository: CloudSyncRepository,
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun execute(accessToken: String, fileId: String, driveModifiedAtMs: Long): ImportSqliteDatabaseResult {
        val tempFile = cloudSyncRepository.downloadBackupToTempFile(accessToken, fileId)
        return try {
            val result = backupRepository.importBackup(tempFile.inputStream())
            if (result is DatabaseBackupImportResult.ReplacementApplied) {
                settingsRepository.setLastSyncedEpochMs(driveModifiedAtMs)
                settingsRepository.setAutoSyncErrorStatus(null)
            }
            when (result) {
                DatabaseBackupImportResult.ReplacementApplied -> ImportSqliteDatabaseResult.RestartPending
                DatabaseBackupImportResult.InvalidFile -> ImportSqliteDatabaseResult.InvalidFile
                is DatabaseBackupImportResult.IoFailure -> ImportSqliteDatabaseResult.Error(result.message)
            }
        } finally {
            tempFile.delete()
        }
    }
}
