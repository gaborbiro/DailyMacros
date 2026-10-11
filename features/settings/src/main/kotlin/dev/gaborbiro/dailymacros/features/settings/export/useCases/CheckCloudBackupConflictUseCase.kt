package dev.gaborbiro.dailymacros.features.settings.export.useCases

import dev.gaborbiro.dailymacros.repositories.backup.domain.model.DriveBackupInfo
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import javax.inject.Inject

class CheckCloudBackupConflictUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * True if the cloud backup holds a write this install hasn't seen, i.e. uploading now would
     * overwrite another device's data.
     *
     * A newer backup written by this install itself is not a conflict: it's an upload that
     * reached Drive but whose success was never recorded locally (connection dropped before the
     * response, coroutine cancelled or process killed mid-upload, or two syncs racing). It's
     * adopted as the last synced state instead.
     */
    fun execute(driveInfo: DriveBackupInfo?): Boolean {
        if (driveInfo == null) return false
        if (driveInfo.modifiedTimeMs <= (settingsRepository.getLastSyncedEpochMs() ?: 0L)) return false
        if (driveInfo.writtenByThisInstall) {
            settingsRepository.setLastSyncedEpochMs(driveInfo.modifiedTimeMs)
            return false
        }
        return true
    }
}
