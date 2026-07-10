package dev.gaborbiro.dailymacros.features.settings.model

import dev.gaborbiro.dailymacros.repositories.settings.domain.model.BackupInterval
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.CloudSyncProvider

data class SettingsUiState(
    val showTargetsSettings: Boolean,
    val bottomLabel: String,
    val exportDataInProgress: Boolean = false,
    val importDataInProgress: Boolean = false,
    val diaryDayStartHour: Int = 0,
    val showDiaryDayStartDialog: Boolean = false,
    val showPromptEditor: Boolean = false,
    val cloudSyncProvider: CloudSyncProvider = CloudSyncProvider.NONE,
    val cloudSyncEmail: String? = null,
    val lastSyncedEpochMs: Long? = null,
    val cloudSyncInProgress: Boolean = false,
    val showSignOutConfirmDialog: Boolean = false,
    val showRestoreConfirmDialog: Boolean = false,
    val restoreDialogModifiedAtMs: Long = 0L,
    val restoreDialogFileId: String = "",
    val customiseAiEnabled: Boolean = false,
    val aiInsightsEnabled: Boolean = false,
    val autoPhotoRecognitionEnabled: Boolean = false,
    val autoBackupInterval: BackupInterval = BackupInterval.NEVER,
    val showAutoBackupIntervalDialog: Boolean = false,
    val showOverwriteConfirmDialog: Boolean = false,
    val overwriteDialogDriveModifiedAtMs: Long = 0L,
)
