package dev.gaborbiro.dailymacros.features.settings.export.useCases

import dev.gaborbiro.dailymacros.repositories.backup.domain.BackupRepository
import dev.gaborbiro.dailymacros.repositories.backup.domain.CloudSyncRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import javax.inject.Inject

class SyncDatabaseUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun execute(accessToken: String) {
        val tarFile = backupRepository.prepareBackupArchiveForExport()
        try {
            val uploaded = cloudSyncRepository.uploadBackup(accessToken, tarFile)
            settingsRepository.setLastSyncedEpochMs(uploaded.modifiedTimeMs)
            settingsRepository.setLastBackupAttemptEpochMs(System.currentTimeMillis())
            settingsRepository.setAutoSyncErrorStatus(null)
        } finally {
            tarFile.delete()
        }
    }
}
