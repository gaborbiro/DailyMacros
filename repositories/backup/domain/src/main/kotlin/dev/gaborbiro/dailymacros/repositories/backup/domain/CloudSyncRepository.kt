package dev.gaborbiro.dailymacros.repositories.backup.domain

import dev.gaborbiro.dailymacros.repositories.backup.domain.model.DriveBackupInfo
import java.io.File

interface CloudSyncRepository {

    /** Returns null if no backup exists in the Drive appDataFolder. */
    suspend fun getBackupInfo(accessToken: String): DriveBackupInfo?

    /** Uploads [tarFile] to Drive appDataFolder, overwriting any existing backup. */
    suspend fun uploadBackup(accessToken: String, tarFile: File): DriveBackupInfo

    /**
     * Downloads the backup into a temp file in cacheDir.
     * Caller is responsible for deleting the returned file when done.
     */
    suspend fun downloadBackupToTempFile(accessToken: String, fileId: String): File
}
