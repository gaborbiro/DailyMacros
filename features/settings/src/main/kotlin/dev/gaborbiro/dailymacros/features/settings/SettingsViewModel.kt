package dev.gaborbiro.dailymacros.features.settings

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import dev.gaborbiro.dailymacros.core.featureflags.FeatureFlagStore
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dev.gaborbiro.dailymacros.features.settings.DRIVE_SCOPE_TOKEN
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.dailymacros.features.settings.export.CreatePublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.shared.photodiary.PhotoMonitorWorker
import dev.gaborbiro.dailymacros.features.settings.export.OpenPublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.export.pdf.DiaryDateRange
import dev.gaborbiro.dailymacros.features.settings.export.pdf.PdfRangeSelection
import dev.gaborbiro.dailymacros.features.settings.export.pdf.computeRange
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportPdfDiaryUseCase
import dev.gaborbiro.dailymacros.features.settings.export.useCases.PdfExportResult
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportSqliteDatabaseUseCase
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ImportSqliteDatabaseResult
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ImportSqliteDatabaseUseCase
import dev.gaborbiro.dailymacros.features.settings.export.useCases.RestoreFromDriveUseCase
import dev.gaborbiro.dailymacros.features.settings.export.useCases.SyncDatabaseUseCase
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiState
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiUpdates
import dev.gaborbiro.dailymacros.repositories.backup.domain.CloudSyncRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.BackupInterval
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.CloudSyncProvider
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PdfExportOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    appInfo: SettingsAppInfo,
    private val settingsRepository: SettingsRepository,
    private val exportPdfDiaryUseCase: ExportPdfDiaryUseCase,
    private val exportSqliteDatabaseUseCase: ExportSqliteDatabaseUseCase,
    private val importSqliteDatabaseUseCase: ImportSqliteDatabaseUseCase,
    private val syncDatabaseUseCase: SyncDatabaseUseCase,
    private val restoreFromDriveUseCase: RestoreFromDriveUseCase,
    private val cloudSyncRepository: CloudSyncRepository,
    private val featureFlagStore: FeatureFlagStore,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            showTargetsSettings = false,
            bottomLabel = appInfo.versionLabel,
            diaryDayStartHour = settingsRepository.getDiaryDayStartHour(),
            cloudSyncProvider = settingsRepository.getCloudSyncProvider(),
            cloudSyncEmail = settingsRepository.getCloudSyncEmail(),
            lastSyncedEpochMs = settingsRepository.getLastSyncedEpochMs(),
            customiseAiEnabled = featureFlagStore.isEnabled(FeatureFlagStore.Key.CUSTOMISE_AI_ENABLED),
            aiInsightsEnabled = featureFlagStore.isEnabled(FeatureFlagStore.Key.AI_INSIGHTS_ENABLED),
            autoPhotoRecognitionEnabled = settingsRepository.getAutoPhotoRecognitionEnabled(),
            autoPhotoRecognitionVisible = featureFlagStore.isEnabled(FeatureFlagStore.Key.AUTO_PHOTO_RECOGNITION_ENABLED),
            quickPickConfirmationEnabled = settingsRepository.getQuickPickConfirmationEnabled(),
            autoBackupInterval = settingsRepository.getAutoBackupInterval(),
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<SettingsUiUpdates>()
    val uiUpdates: SharedFlow<SettingsUiUpdates> = _uiUpdates.asSharedFlow()

    /**
     * Called after each auto-sync attempt. Re-reads the persisted timestamp, then refreshes the
     * displayed value from the cloud backup itself so overwrites by other devices show up.
     * The fetched cloud timestamp is display-only: persisting it into lastSyncedEpochMs would
     * defeat conflict detection, which compares the cloud timestamp against the persisted one.
     */
    fun onAutoSyncFinished() {
        _uiState.update { it.copy(lastSyncedEpochMs = settingsRepository.getLastSyncedEpochMs()) }
        if (settingsRepository.getCloudSyncProvider() == CloudSyncProvider.NONE) return
        viewModelScope.launch {
            runCatching {
                val token = getDriveAccessToken() ?: return@launch
                val info = cloudSyncRepository.getBackupInfo(token)
                if (info != null) {
                    _uiState.update { it.copy(lastSyncedEpochMs = info.modifiedTimeMs) }
                }
            }.onFailure { t ->
                Log.e("CloudSync", "Failed to refresh cloud backup timestamp", t)
            }
        }
    }

    fun onBackNavigateRequested() {
        _uiState.update { it.copy(showTargetsSettings = false) }
        viewModelScope.launch { _uiUpdates.emit(SettingsUiUpdates.NavigateBack) }
    }

    fun onTargetsSettingsTapped() {
        _uiState.update { it.copy(showTargetsSettings = true) }
    }

    fun onTargetsSettingsCloseRequested() {
        _uiState.update { it.copy(showTargetsSettings = false) }
    }

    fun onPromptEditorTapped() {
        _uiState.update { it.copy(showPromptEditor = true) }
    }

    fun onPromptEditorCloseRequested() {
        _uiState.update { it.copy(showPromptEditor = false) }
    }

    fun onDiaryDayStartRowTapped() {
        _uiState.update { it.copy(showDiaryDayStartDialog = true) }
    }

    fun onDiaryDayStartDialogDismissed() {
        _uiState.update { it.copy(showDiaryDayStartDialog = false) }
    }

    fun onDiaryDayStartHourSelected(hourOfDay: Int) {
        val hour = hourOfDay.coerceIn(0, 2)
        settingsRepository.setDiaryDayStartHour(hour)
        _uiState.update { it.copy(diaryDayStartHour = hour, showDiaryDayStartDialog = false) }
    }

    fun onExportSettingsTapped() {
        _uiState.update {
            it.copy(
                showPdfExportDialog = true,
                pdfExportOptions = settingsRepository.getPdfExportOptions(),
            )
        }
    }

    fun onPdfExportDialogDismissed() {
        _uiState.update { it.copy(showPdfExportDialog = false) }
    }

    fun onPdfExportConfirmed(
        createPublicDocumentUseCase: CreatePublicDocumentUseCase,
        selection: PdfRangeSelection,
        options: PdfExportOptions,
    ) {
        _uiState.update { it.copy(showPdfExportDialog = false) }
        val range = resolveRange(selection)
        viewModelScope.launch {
            _uiState.update { it.copy(pdfExportInProgress = true) }
            runCatching { exportPdfDiaryUseCase.execute(createPublicDocumentUseCase, range, options) }
                .onSuccess { result ->
                    when (result) {
                        PdfExportResult.Enqueued ->
                            _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Exporting… we'll notify you when the PDF is ready."))
                        PdfExportResult.Empty ->
                            _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("No entries in that date range."))
                        PdfExportResult.Cancelled -> Unit
                    }
                }
                .onFailure { t ->
                    Log.e("PdfExport", "PDF export failed", t)
                    _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Export failed: ${t.message ?: t.toString()}"))
                }
            _uiState.update { it.copy(pdfExportInProgress = false) }
        }
    }

    private fun resolveRange(selection: PdfRangeSelection): DiaryDateRange = when (selection) {
        is PdfRangeSelection.Custom ->
            if (selection.from <= selection.to) {
                DiaryDateRange(selection.from, selection.to)
            } else {
                DiaryDateRange(selection.to, selection.from)
            }

        is PdfRangeSelection.Preset -> {
            val zone = java.time.ZoneId.systemDefault()
            val dayStart = dev.gaborbiro.dailymacros.features.common.utils
                .diaryDayStartTime(settingsRepository.getDiaryDayStartHour())
            val today = dev.gaborbiro.dailymacros.features.common.utils.logicalDiaryToday(zone, dayStart)
            val firstDayOfWeek = java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).firstDayOfWeek
            computeRange(selection.preset, today, firstDayOfWeek)
        }
    }

    fun onExportDbTapped(createPublicDocumentUseCase: CreatePublicDocumentUseCase) {
        viewModelScope.launch {
            _uiState.update { it.copy(exportDataInProgress = true) }
            runCatching { exportSqliteDatabaseUseCase.execute(createPublicDocumentUseCase) }
                .onSuccess { saved ->
                    if (saved) {
                        _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Backup saved."))
                    }
                }
                .onFailure { t -> _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar(t.message ?: t.toString())) }
            _uiState.update { it.copy(exportDataInProgress = false) }
        }
    }

    fun onImportDbTapped(openPublicDocumentUseCase: OpenPublicDocumentUseCase) {
        viewModelScope.launch {
            _uiState.update { it.copy(importDataInProgress = true) }
            when (val result = importSqliteDatabaseUseCase.execute(openPublicDocumentUseCase)) {
                ImportSqliteDatabaseResult.Cancelled -> Unit
                ImportSqliteDatabaseResult.InvalidFile ->
                    _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("That file is not a valid backup (.tar)"))
                ImportSqliteDatabaseResult.RestartPending ->
                    _uiUpdates.emit(SettingsUiUpdates.RestartApplication)
                is ImportSqliteDatabaseResult.Error ->
                    _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar(result.message))
            }
            _uiState.update { it.copy(importDataInProgress = false) }
        }
    }

    // ---- Cloud sync ----

    private var restoreAfterSignIn = false

    fun onCloudSyncRowTapped() {
        val current = _uiState.value.cloudSyncProvider
        if (current == CloudSyncProvider.NONE) {
            viewModelScope.launch { _uiUpdates.emit(SettingsUiUpdates.RequestGoogleSignIn) }
        } else {
            _uiState.update { it.copy(showSignOutConfirmDialog = true) }
        }
    }

    fun onCloudSyncForRestoreTapped() {
        val current = _uiState.value.cloudSyncProvider
        if (current == CloudSyncProvider.NONE) {
            restoreAfterSignIn = true
            viewModelScope.launch { _uiUpdates.emit(SettingsUiUpdates.RequestGoogleSignIn) }
        } else {
            onRestoreFromDriveTappedFromOverview()
        }
    }

    fun onSignOutConfirmed() {
        _uiState.update { it.copy(showSignOutConfirmDialog = false) }
        signOut()
    }

    fun onSignOutDialogDismissed() {
        _uiState.update { it.copy(showSignOutConfirmDialog = false) }
    }

    fun onGoogleSignInSuccess(email: String) {
        settingsRepository.setCloudSyncProvider(CloudSyncProvider.GOOGLE_DRIVE)
        settingsRepository.setCloudSyncEmail(email)
        _uiState.update {
            it.copy(
                cloudSyncProvider = CloudSyncProvider.GOOGLE_DRIVE,
                cloudSyncEmail = email,
            )
        }
        if (restoreAfterSignIn) {
            restoreAfterSignIn = false
            onRestoreFromDriveTappedFromOverview()
        } else {
            viewModelScope.launch {
                runCatching {
                    val token = getDriveAccessToken() ?: return@launch
                    val info = cloudSyncRepository.getBackupInfo(token)
                    if (info != null) {
                        // Display-only: persisting the cloud timestamp would mark this device as
                        // in-sync with a backup it never restored, disabling conflict detection
                        // and letting the next sync overwrite the backup.
                        _uiState.update { it.copy(lastSyncedEpochMs = info.modifiedTimeMs) }
                    }
                }.onFailure { t ->
                    Log.e("CloudSync", "Failed to check existing backup", t)
                }
            }
        }
    }

    fun onRestoreFromDriveTappedFromOverview() {
        viewModelScope.launch {
            _uiState.update { it.copy(cloudSyncInProgress = true) }
            runCatching {
                val token = getDriveAccessToken() ?: run {
                    _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Not signed in. Tap Cloud sync to sign in."))
                    return@launch
                }
                val info = cloudSyncRepository.getBackupInfo(token)
                if (info == null) {
                    _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("No backup found on Google Drive."))
                } else {
                    when (restoreFromDriveUseCase.execute(token, info.fileId, info.modifiedTimeMs)) {
                        ImportSqliteDatabaseResult.RestartPending ->
                            _uiUpdates.emit(SettingsUiUpdates.RestartApplication)
                        ImportSqliteDatabaseResult.InvalidFile ->
                            _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Remote backup file is invalid."))
                        is ImportSqliteDatabaseResult.Error ->
                            _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Restore failed."))
                        ImportSqliteDatabaseResult.Cancelled -> Unit
                    }
                }
            }.onFailure { t ->
                Log.e("CloudSync", "Restore failed", t)
                _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Restore failed: ${t.message}"))
            }
            _uiState.update { it.copy(cloudSyncInProgress = false) }
        }
    }

    fun onGoogleSignInFailed(message: String) {
        viewModelScope.launch { _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Sign-in failed: $message")) }
    }

    fun onSyncTapped() {
        viewModelScope.launch {
            _uiState.update { it.copy(cloudSyncInProgress = true) }
            runCatching {
                val token = getDriveAccessToken() ?: run {
                    _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Not signed in. Tap Cloud sync to sign in."))
                    return@launch
                }
                val driveInfo = cloudSyncRepository.getBackupInfo(token)
                if (driveInfo != null) {
                    // Display-only, like onAutoSyncFinished: must not be persisted, or conflict
                    // detection would treat this device as in sync with the cloud backup.
                    _uiState.update { it.copy(lastSyncedEpochMs = driveInfo.modifiedTimeMs) }
                }
                val lastSynced = settingsRepository.getLastSyncedEpochMs()
                if (driveInfo != null && driveInfo.modifiedTimeMs > (lastSynced ?: 0L)) {
                    _uiState.update {
                        it.copy(
                            cloudSyncInProgress = false,
                            showOverwriteConfirmDialog = true,
                            overwriteDialogDriveModifiedAtMs = driveInfo.modifiedTimeMs,
                        )
                    }
                    return@launch
                }
                uploadBackup(token)
            }.onFailure { t ->
                Log.e("CloudSync", "Backup failed", t)
                _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Backup failed: ${t.message}"))
            }
            _uiState.update { it.copy(cloudSyncInProgress = false) }
        }
    }

    fun onOverwriteConfirmed() {
        _uiState.update { it.copy(showOverwriteConfirmDialog = false) }
        viewModelScope.launch {
            _uiState.update { it.copy(cloudSyncInProgress = true) }
            runCatching {
                val token = getDriveAccessToken() ?: run {
                    _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Not signed in. Tap Cloud sync to sign in."))
                    return@launch
                }
                uploadBackup(token)
            }.onFailure { t ->
                Log.e("CloudSync", "Backup failed", t)
                _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Backup failed: ${t.message}"))
            }
            _uiState.update { it.copy(cloudSyncInProgress = false) }
        }
    }

    fun onOverwriteDialogDismissed() {
        _uiState.update { it.copy(showOverwriteConfirmDialog = false) }
    }

    private suspend fun uploadBackup(token: String) {
        syncDatabaseUseCase.execute(token)
        val newTs = settingsRepository.getLastSyncedEpochMs()
        _uiState.update { it.copy(lastSyncedEpochMs = newTs) }
        _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Backup uploaded to Google Drive."))
    }

    fun onRestoreFromDriveTapped() {
        viewModelScope.launch {
            _uiState.update { it.copy(cloudSyncInProgress = true) }
            runCatching {
                val token = getDriveAccessToken() ?: run {
                    _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Not signed in. Tap Cloud sync to sign in."))
                    return@launch
                }
                val info = cloudSyncRepository.getBackupInfo(token)
                if (info == null) {
                    _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("No backup found on Google Drive."))
                } else {
                    _uiState.update {
                        it.copy(
                            // Display-only refresh; see onSyncTapped.
                            lastSyncedEpochMs = info.modifiedTimeMs,
                            showRestoreConfirmDialog = true,
                            restoreDialogModifiedAtMs = info.modifiedTimeMs,
                            restoreDialogFileId = info.fileId,
                        )
                    }
                }
            }.onFailure { t ->
                Log.e("CloudSync", "Failed to check backup", t)
                _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Failed to check backup: ${t.message}"))
            }
            _uiState.update { it.copy(cloudSyncInProgress = false) }
        }
    }

    fun onRestoreConfirmed() {
        val state = _uiState.value
        _uiState.update { it.copy(showRestoreConfirmDialog = false, cloudSyncInProgress = true) }
        viewModelScope.launch {
            runCatching {
                val token = getDriveAccessToken() ?: run {
                    _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Not signed in."))
                    return@launch
                }
                when (restoreFromDriveUseCase.execute(token, state.restoreDialogFileId, state.restoreDialogModifiedAtMs)) {
                    ImportSqliteDatabaseResult.RestartPending ->
                        _uiUpdates.emit(SettingsUiUpdates.RestartApplication)
                    ImportSqliteDatabaseResult.InvalidFile ->
                        _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Remote backup file is invalid."))
                    is ImportSqliteDatabaseResult.Error ->
                        _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Restore failed."))
                    ImportSqliteDatabaseResult.Cancelled -> Unit
                }
            }.onFailure { t ->
                Log.e("CloudSync", "Restore failed", t)
                _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Restore failed: ${t.message}"))
            }
            _uiState.update { it.copy(cloudSyncInProgress = false) }
        }
    }

    fun onRestoreDialogDismissed() {
        _uiState.update { it.copy(showRestoreConfirmDialog = false) }
    }

    fun onAutoBackupIntervalRowTapped() {
        _uiState.update { it.copy(showAutoBackupIntervalDialog = true) }
    }

    fun onAutoBackupIntervalDialogDismissed() {
        _uiState.update { it.copy(showAutoBackupIntervalDialog = false) }
    }

    fun onAutoBackupIntervalSelected(interval: BackupInterval) {
        settingsRepository.setAutoBackupInterval(interval)
        _uiState.update { it.copy(autoBackupInterval = interval, showAutoBackupIntervalDialog = false) }
    }

    fun onAutoPhotoRecognitionToggled(enabled: Boolean) {
        if (enabled) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            val granted = ContextCompat.checkSelfPermission(
                getApplication(),
                permission,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                enableAutoPhotoRecognition()
            } else {
                viewModelScope.launch { _uiUpdates.emit(SettingsUiUpdates.RequestPhotoPermissions) }
            }
        } else {
            settingsRepository.setAutoPhotoRecognitionEnabled(false)
            PhotoMonitorWorker.cancel(getApplication())
            _uiState.update { it.copy(autoPhotoRecognitionEnabled = false) }
        }
    }

    fun onQuickPickConfirmationToggled(enabled: Boolean) {
        settingsRepository.setQuickPickConfirmationEnabled(enabled)
        _uiState.update { it.copy(quickPickConfirmationEnabled = enabled) }
    }

    fun onAutoPhotoPermissionsGranted() {
        enableAutoPhotoRecognition()
    }

    fun onAutoPhotoPermissionsDenied() {
        _uiState.update { it.copy(autoPhotoRecognitionEnabled = false) }
        viewModelScope.launch {
            _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Permission required to monitor camera photos."))
        }
    }

    private fun enableAutoPhotoRecognition() {
        settingsRepository.setAutoPhotoRecognitionEnabled(true)
        PhotoMonitorWorker.enqueue(getApplication())
        _uiState.update { it.copy(autoPhotoRecognitionEnabled = true) }
    }

    private fun signOut() {
        GoogleSignIn.getClient(getApplication(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        settingsRepository.setCloudSyncProvider(CloudSyncProvider.NONE)
        settingsRepository.setCloudSyncEmail(null)
        settingsRepository.setLastSyncedEpochMs(null)
        settingsRepository.setAutoSyncErrorStatus(null)
        _uiState.update {
            it.copy(
                cloudSyncProvider = CloudSyncProvider.NONE,
                cloudSyncEmail = null,
                lastSyncedEpochMs = null,
            )
        }
    }

    private suspend fun getDriveAccessToken(): String? = withContext(Dispatchers.IO) {
        val app = getApplication<Application>()
        val account = GoogleSignIn.getLastSignedInAccount(app) ?: return@withContext null
        try {
            GoogleAuthUtil.getToken(app, account.account!!, DRIVE_SCOPE_TOKEN)
        } catch (e: UserRecoverableAuthException) {
            null
        } catch (e: GoogleAuthException) {
            null
        }
    }

}
