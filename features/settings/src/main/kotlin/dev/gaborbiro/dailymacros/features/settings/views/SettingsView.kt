package dev.gaborbiro.dailymacros.features.settings.views

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.features.settings.R
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiState
import dev.gaborbiro.dailymacros.features.settings.util.verticalScrollWithBar
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.BackupInterval
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.CloudSyncProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsView(
    viewState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    highlightTargets: Boolean = false,
    onBackNavigateRequested: () -> Unit,
    onTargetsSettingTapped: () -> Unit,
    onPromptEditorTapped: () -> Unit,
    onDiaryDayStartTapped: () -> Unit,
    onDiaryDayStartDialogDismissed: () -> Unit,
    onDiaryDayStartHourSelected: (Int) -> Unit,
    onAutoPhotoRecognitionToggled: (Boolean) -> Unit,
    onExportSettingTapped: () -> Unit,
    onExportDbTapped: () -> Unit,
    onImportDbTapped: () -> Unit,
    onCloudSyncTapped: () -> Unit,
    onSignOutConfirmed: () -> Unit,
    onSignOutDialogDismissed: () -> Unit,
    onSyncTapped: () -> Unit,
    onRestoreFromDriveTapped: () -> Unit,
    onRestoreConfirmed: () -> Unit,
    onRestoreDialogDismissed: () -> Unit,
    onAutoBackupIntervalTapped: () -> Unit,
    onAutoBackupIntervalSelected: (BackupInterval) -> Unit,
    onAutoBackupIntervalDialogDismissed: () -> Unit,
    onOverwriteConfirmed: () -> Unit,
    onOverwriteDialogDismissed: () -> Unit,
) {

    if (viewState.showDiaryDayStartDialog) {
        AlertDialog(
            onDismissRequest = onDiaryDayStartDialogDismissed,
            title = {
                Text(text = stringResource(R.string.settings_diary_day_start_dialog_title))
            },
            text = {
                Column {
                    listOf(
                        0 to R.string.settings_diary_day_start_option_midnight,
                        1 to R.string.settings_diary_day_start_option_1am,
                        2 to R.string.settings_diary_day_start_option_2am,
                    ).forEach { (hour, labelRes) ->
                        TextButton(
                            onClick = { onDiaryDayStartHourSelected(hour) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(labelRes))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDiaryDayStartDialogDismissed) {
                    Text(stringResource(R.string.settings_diary_day_start_dialog_close))
                }
            },
        )
    }

    if (viewState.showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = onSignOutDialogDismissed,
            title = { Text(stringResource(R.string.settings_dialog_sign_out_title)) },
            text = { Text(stringResource(R.string.settings_dialog_sign_out_message, viewState.cloudSyncEmail ?: "your Google account")) },
            confirmButton = {
                TextButton(onClick = onSignOutConfirmed) { Text(stringResource(R.string.settings_dialog_sign_out_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = onSignOutDialogDismissed) { Text(stringResource(R.string.settings_dialog_cancel)) }
            },
        )
    }

    if (viewState.showRestoreConfirmDialog) {
        val dateStr = remember(viewState.restoreDialogModifiedAtMs) {
            SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                .format(Date(viewState.restoreDialogModifiedAtMs))
        }
        AlertDialog(
            onDismissRequest = onRestoreDialogDismissed,
            title = { Text(stringResource(R.string.settings_dialog_restore_title)) },
            text = { Text(stringResource(R.string.settings_dialog_restore_message, dateStr)) },
            confirmButton = {
                TextButton(onClick = onRestoreConfirmed) { Text(stringResource(R.string.settings_dialog_restore_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = onRestoreDialogDismissed) { Text(stringResource(R.string.settings_dialog_cancel)) }
            },
        )
    }

    if (viewState.showAutoBackupIntervalDialog) {
        AlertDialog(
            onDismissRequest = onAutoBackupIntervalDialogDismissed,
            title = {
                Text(text = stringResource(R.string.settings_cloud_sync_auto_backup_dialog_title))
            },
            text = {
                Column {
                    listOf(
                        BackupInterval.NEVER to R.string.settings_cloud_sync_auto_backup_never,
                        BackupInterval.DAILY to R.string.settings_cloud_sync_auto_backup_daily,
                        BackupInterval.WEEKLY to R.string.settings_cloud_sync_auto_backup_weekly,
                        BackupInterval.MONTHLY to R.string.settings_cloud_sync_auto_backup_monthly,
                    ).forEach { (interval, labelRes) ->
                        TextButton(
                            onClick = { onAutoBackupIntervalSelected(interval) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(labelRes))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onAutoBackupIntervalDialogDismissed) {
                    Text(stringResource(R.string.settings_diary_day_start_dialog_close))
                }
            },
        )
    }

    if (viewState.showOverwriteConfirmDialog) {
        val dateStr = remember(viewState.overwriteDialogDriveModifiedAtMs) {
            SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                .format(Date(viewState.overwriteDialogDriveModifiedAtMs))
        }
        AlertDialog(
            onDismissRequest = onOverwriteDialogDismissed,
            title = { Text(stringResource(R.string.settings_dialog_overwrite_title)) },
            text = { Text(stringResource(R.string.settings_dialog_overwrite_message, dateStr)) },
            confirmButton = {
                TextButton(onClick = onOverwriteConfirmed) { Text(stringResource(R.string.settings_dialog_overwrite_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = onOverwriteDialogDismissed) { Text(stringResource(R.string.settings_dialog_cancel)) }
            },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_content_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackNavigateRequested) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.settings_content_back_cd),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val targetsHighlight = remember { Animatable(0f) }
        LaunchedEffect(highlightTargets) {
            if (highlightTargets) {
                delay(400)
                repeat(2) {
                    targetsHighlight.animateTo(1f, tween(250))
                    targetsHighlight.animateTo(0f, tween(350))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScrollWithBar(),
        ) {
            SettingRow(title = stringResource(R.string.settings_content_daily_targets_row), highlight = targetsHighlight.value, onTapped = onTargetsSettingTapped)
            if (viewState.customiseAiEnabled) {
                SettingRow(title = stringResource(R.string.settings_content_customise_ai_row), subtitle = stringResource(R.string.settings_content_customise_ai_subtitle), onTapped = onPromptEditorTapped)
            }
            SettingRow(
                title = stringResource(R.string.settings_diary_day_start_row),
                subtitle = diaryDayStartSummary(viewState.diaryDayStartHour),
                onTapped = onDiaryDayStartTapped,
            )
            SettingRow(title = stringResource(R.string.settings_content_export_diary_row), onTapped = onExportSettingTapped)

            SettingSectionHeader(title = stringResource(R.string.settings_content_camera_section))
            SettingRow(
                title = stringResource(R.string.settings_auto_photo_recognition_row),
                subtitle = stringResource(R.string.settings_auto_photo_recognition_subtitle),
                onTapped = { onAutoPhotoRecognitionToggled(!viewState.autoPhotoRecognitionEnabled) },
                trailing = {
                    Switch(
                        checked = viewState.autoPhotoRecognitionEnabled,
                        onCheckedChange = onAutoPhotoRecognitionToggled,
                    )
                },
            )

            SettingSectionHeader(title = stringResource(R.string.settings_content_local_sync_section))
            val localSyncIdle = !viewState.exportDataInProgress && !viewState.importDataInProgress
            SettingRow(
                title = stringResource(R.string.settings_content_backup_now_row),
                onTapped = onExportDbTapped,
                enabled = localSyncIdle,
                trailing = {
                    if (viewState.exportDataInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                },
            )
            SettingRow(
                title = stringResource(R.string.settings_content_restore_backup_row),
                onTapped = onImportDbTapped,
                enabled = localSyncIdle,
                trailing = {
                    if (viewState.importDataInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                },
            )

            val isSignedIn = viewState.cloudSyncProvider != CloudSyncProvider.NONE
            val cloudSyncIdle = !viewState.cloudSyncInProgress
            SettingSectionHeader(title = stringResource(R.string.settings_content_cloud_sync_section))
            SettingRow(
                title = stringResource(R.string.settings_content_account_row),
                subtitle = if (isSignedIn) viewState.cloudSyncEmail else stringResource(R.string.settings_content_sign_in_subtitle),
                enabled = cloudSyncIdle,
                onTapped = onCloudSyncTapped,
                trailing = {
                    if (viewState.cloudSyncInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                },
            )
            if (isSignedIn) {
                val lastSyncedText = if (viewState.lastSyncedEpochMs != null) {
                    stringResource(
                        R.string.settings_content_last_backed_up,
                        SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(viewState.lastSyncedEpochMs))
                    )
                } else {
                    stringResource(R.string.settings_content_never_backed_up)
                }
                SettingRow(
                    title = stringResource(R.string.settings_content_backup_now_row),
                    subtitle = lastSyncedText,
                    enabled = cloudSyncIdle,
                    onTapped = onSyncTapped,
                )
                SettingRow(
                    title = stringResource(R.string.settings_cloud_sync_auto_backup_row),
                    subtitle = stringResource(viewState.autoBackupInterval.toLabelRes()),
                    enabled = cloudSyncIdle,
                    onTapped = onAutoBackupIntervalTapped,
                )
                SettingRow(
                    title = stringResource(R.string.settings_content_restore_backup_row),
                    enabled = cloudSyncIdle,
                    onTapped = onRestoreFromDriveTapped,
                )
            }

            SelectionContainer {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .padding(WindowInsets.navigationBars.asPaddingValues()),
                    text = viewState.bottomLabel,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun diaryDayStartSummary(hour: Int): String {
    val h = hour.coerceIn(0, 2)
    return when (h) {
        0 -> stringResource(R.string.settings_diary_day_start_option_midnight)
        1 -> stringResource(R.string.settings_diary_day_start_option_1am)
        else -> stringResource(R.string.settings_diary_day_start_option_2am)
    }
}

@Composable
private fun SettingSectionHeader(title: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    highlight: Float = 0f,
    trailing: @Composable (() -> Unit)? = null,
    onTapped: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = highlight))
            .clickable(
                enabled = enabled,
                onClick = onTapped,
            )
            .alpha(if (enabled) 1f else 0.6f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
        ) {
            Text(text = title)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
        Spacer(modifier = Modifier.padding(end = 16.dp))
    }
}

private fun BackupInterval.toLabelRes(): Int = when (this) {
    BackupInterval.NEVER -> R.string.settings_cloud_sync_auto_backup_never
    BackupInterval.DAILY -> R.string.settings_cloud_sync_auto_backup_daily
    BackupInterval.WEEKLY -> R.string.settings_cloud_sync_auto_backup_weekly
    BackupInterval.MONTHLY -> R.string.settings_cloud_sync_auto_backup_monthly
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SettingsViewPreview() {
    SettingsPreviewContext {
        SettingsView(
            viewState = SettingsUiState(
                showTargetsSettings = true,
                bottomLabel = "Bottom Label",
                diaryDayStartHour = 1,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBackNavigateRequested = {},
            onTargetsSettingTapped = {},
            onPromptEditorTapped = {},
            onDiaryDayStartTapped = {},
            onDiaryDayStartDialogDismissed = {},
            onDiaryDayStartHourSelected = {},
            onAutoPhotoRecognitionToggled = {},
            onExportSettingTapped = {},
            onExportDbTapped = {},
            onImportDbTapped = {},
            onCloudSyncTapped = {},
            onSignOutConfirmed = {},
            onSignOutDialogDismissed = {},
            onSyncTapped = {},
            onRestoreFromDriveTapped = {},
            onRestoreConfirmed = {},
            onRestoreDialogDismissed = {},
            onAutoBackupIntervalTapped = {},
            onAutoBackupIntervalSelected = {},
            onAutoBackupIntervalDialogDismissed = {},
            onOverwriteConfirmed = {},
            onOverwriteDialogDismissed = {},
        )
    }
}
