package dev.gaborbiro.dailymacros.features.settings.export.useCases

import dev.gaborbiro.dailymacros.repositories.backup.domain.BackupRepository
import dev.gaborbiro.dailymacros.repositories.backup.domain.CloudSyncRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import javax.inject.Inject

sealed class SyncResult {
    data object Uploaded : SyncResult()
    data class RemoteIsNewer(val modifiedTimeMs: Long, val fileId: String) : SyncResult()
}

class SyncDatabaseUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun execute(accessToken: String): SyncResult {
        val lastSynced = settingsRepository.getLastSyncedEpochMs() ?: 0L
        val remoteInfo = cloudSyncRepository.getBackupInfo(accessToken)

        if (remoteInfo != null && remoteInfo.modifiedTimeMs > lastSynced + CLOCK_SKEW_MS) {
            return SyncResult.RemoteIsNewer(remoteInfo.modifiedTimeMs, remoteInfo.fileId)
        }

        val tarFile = backupRepository.prepareBackupArchiveForExport()
        try {
            val uploaded = cloudSyncRepository.uploadBackup(accessToken, tarFile)
            settingsRepository.setLastSyncedEpochMs(uploaded.modifiedTimeMs)
        } finally {
            tarFile.delete()
        }
        return SyncResult.Uploaded
    }

    private companion object {
        const val CLOCK_SKEW_MS = 5_000L
    }
}
