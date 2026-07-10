package dev.gaborbiro.dailymacros.features.settings.export.useCases

import android.content.Context
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.BackupInterval
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.CloudSyncProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AutoSyncUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
) {
    sealed interface Result {
        data object Skipped : Result
        data object Success : Result
        data class Failure(val message: String) : Result
    }

    suspend fun execute(): Result {
        if (settingsRepository.getCloudSyncProvider() == CloudSyncProvider.NONE) return Result.Skipped

        val interval = settingsRepository.getAutoBackupInterval()
        if (interval == BackupInterval.NEVER) return Result.Skipped

        val lastSynced = settingsRepository.getLastSyncedEpochMs()
        val now = System.currentTimeMillis()
        if (lastSynced != null && (now - lastSynced) < interval.toMillis()) return Result.Skipped

        return try {
            val token = getDriveAccessToken() ?: return Result.Failure("Not signed in to Google")
            syncDatabaseUseCase.execute(token)
            Result.Success
        } catch (e: Exception) {
            Result.Failure(e.message ?: "Unknown error")
        }
    }

    private suspend fun getDriveAccessToken(): String? = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        try {
            GoogleAuthUtil.getToken(context, account.account!!, DRIVE_SCOPE)
        } catch (e: UserRecoverableAuthException) {
            null
        } catch (e: GoogleAuthException) {
            null
        }
    }

    private companion object {
        const val DRIVE_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.appdata"
    }
}

private fun BackupInterval.toMillis(): Long = when (this) {
    BackupInterval.NEVER -> Long.MAX_VALUE
    BackupInterval.DAILY -> 24 * 60 * 60 * 1000L
    BackupInterval.WEEKLY -> 7 * 24 * 60 * 60 * 1000L
    BackupInterval.MONTHLY -> 30L * 24 * 60 * 60 * 1000L
}
