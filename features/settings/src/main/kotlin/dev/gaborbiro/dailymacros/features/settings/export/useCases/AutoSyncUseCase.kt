package dev.gaborbiro.dailymacros.features.settings.export.useCases

import android.content.Context
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.features.settings.DRIVE_SCOPE_TOKEN
import dev.gaborbiro.dailymacros.repositories.backup.domain.CloudSyncRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.AutoSyncError
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.AutoSyncErrorStatus
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.BackupInterval
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.CloudSyncProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AutoSyncUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
) {
    sealed interface Result {
        data object Skipped : Result
        data object Success : Result
        data class ConflictDetected(val shouldNotify: Boolean) : Result
        data class Failure(val message: String, val shouldNotify: Boolean) : Result
    }

    suspend fun execute(): Result {
        if (settingsRepository.getCloudSyncProvider() == CloudSyncProvider.NONE) return Result.Skipped

        val interval = settingsRepository.getAutoBackupInterval()
        if (interval == BackupInterval.NEVER) return Result.Skipped

        val lastAttempt = settingsRepository.getLastBackupAttemptEpochMs()
        val now = System.currentTimeMillis()
        if (lastAttempt != null && (now - lastAttempt) < interval.toMillis()) return Result.Skipped

        return try {
            val token = getDriveAccessToken()
                ?: return Result.Failure(
                    message = "Not signed in to Google",
                    shouldNotify = recordErrorAndDecideNotify(AutoSyncError.FAILURE, interval),
                )
            val lastSynced = settingsRepository.getLastSyncedEpochMs()
            val driveInfo = cloudSyncRepository.getBackupInfo(token)
            if (driveInfo != null && driveInfo.modifiedTimeMs > (lastSynced ?: 0L)) {
                return Result.ConflictDetected(
                    shouldNotify = recordErrorAndDecideNotify(AutoSyncError.CONFLICT, interval),
                )
            }
            syncDatabaseUseCase.execute(token)
            Result.Success
        } catch (e: Exception) {
            Result.Failure(
                message = e.message ?: "Unknown error",
                shouldNotify = recordErrorAndDecideNotify(AutoSyncError.FAILURE, interval),
            )
        }
    }

    /**
     * Notifications are edge-triggered: the user is notified when the error type changes
     * (including from no-error), then again only if the same error is still unresolved after
     * a full backup interval. Sync attempts themselves keep retrying on every call.
     */
    private fun recordErrorAndDecideNotify(error: AutoSyncError, interval: BackupInterval): Boolean {
        val now = System.currentTimeMillis()
        val lastNotified = settingsRepository.getAutoSyncErrorStatus()
        val shouldNotify = lastNotified == null ||
            lastNotified.error != error ||
            (now - lastNotified.notifiedEpochMs) >= interval.toMillis()
        if (shouldNotify) {
            settingsRepository.setAutoSyncErrorStatus(AutoSyncErrorStatus(error = error, notifiedEpochMs = now))
        }
        return shouldNotify
    }

    private suspend fun getDriveAccessToken(): String? = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        try {
            GoogleAuthUtil.getToken(context, account.account!!, DRIVE_SCOPE_TOKEN)
        } catch (e: UserRecoverableAuthException) {
            null
        } catch (e: GoogleAuthException) {
            null
        }
    }
}

private fun BackupInterval.toMillis(): Long = when (this) {
    BackupInterval.NEVER -> Long.MAX_VALUE
    BackupInterval.DAILY -> 24 * 60 * 60 * 1000L
    BackupInterval.WEEKLY -> 7 * 24 * 60 * 60 * 1000L
    BackupInterval.MONTHLY -> 30L * 24 * 60 * 60 * 1000L
}
