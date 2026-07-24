package dev.gaborbiro.dailymacros.features.settings.export.useCases

import dev.gaborbiro.dailymacros.repositories.backup.domain.BackupRepository
import dev.gaborbiro.dailymacros.repositories.backup.domain.CloudSyncRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import java.io.IOException
import javax.inject.Inject

// Tolerates clock skew/latency between this device and Drive's server when deciding whether a
// recovered post-failure modifiedTime belongs to this device's own just-attempted upload.
private const val UPLOAD_ACK_RECOVERY_WINDOW_MS = 30_000L

class SyncDatabaseUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun execute(accessToken: String) {
        val tarFile = backupRepository.prepareBackupArchiveForExport()
        try {
            val attemptStartMs = System.currentTimeMillis()
            val uploaded = try {
                cloudSyncRepository.uploadBackup(accessToken, tarFile)
            } catch (e: IOException) {
                // The upload may have reached Drive and been committed even though we never
                // received the HTTP response (e.g. connection dropped mid-response on flaky
                // wifi). Re-check Drive's actual state: if it now shows a write from within
                // this attempt's window, that's our own write silently succeeding, not a
                // conflict from another device - adopt it instead of surfacing a false
                // failure/conflict on the very next sync attempt.
                val recovered = runCatching { cloudSyncRepository.getBackupInfo(accessToken) }.getOrNull()
                if (recovered != null && recovered.modifiedTimeMs >= attemptStartMs - UPLOAD_ACK_RECOVERY_WINDOW_MS) {
                    recovered
                } else {
                    throw e
                }
            }
            settingsRepository.setLastSyncedEpochMs(uploaded.modifiedTimeMs)
            settingsRepository.setLastBackupAttemptEpochMs(System.currentTimeMillis())
            settingsRepository.setAutoSyncErrorStatus(null)
        } finally {
            tarFile.delete()
        }
    }
}
