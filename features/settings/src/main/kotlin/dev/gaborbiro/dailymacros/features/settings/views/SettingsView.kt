package dev.gaborbiro.dailymacros.features.settings.views

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.CloudSyncProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsView(
    viewState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onBackNavigateRequested: () -> Unit,
    onTargetsSettingTapped: () -> Unit,
    onPromptEditorTapped: () -> Unit,
    onDiaryDayStartTapped: () -> Unit,
    onDiaryDayStartDialogDismissed: () -> Unit,
    onDiaryDayStartHourSelected: (Int) -> Unit,
    onExportSettingTapped: () -> Unit,
    onExportDbTapped: () -> Unit,
    onImportDbTapped: () -> Unit,
    onCloudSyncTapped: () -> Unit,
    onSyncTapped: () -> Unit,
    onRestoreConfirmed: () -> Unit,
    onRestoreDialogDismissed: () -> Unit,
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

    if (viewState.showRestoreConfirmDialog) {
        val dateStr = remember(viewState.restoreDialogModifiedAtMs) {
            SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                .format(Date(viewState.restoreDialogModifiedAtMs))
        }
        AlertDialog(
            onDismissRequest = onRestoreDialogDismissed,
            title = { Text("Restore from backup?") },
            text = {
                Text(
                    "A newer backup from $dateStr is available on Google Drive. " +
                        "Restore it now? This will replace all your current data."
                )
            },
            confirmButton = {
                TextButton(onClick = onRestoreConfirmed) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = onRestoreDialogDismissed) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackNavigateRequested) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back Button",
                        )
                    }
                },
            )
        },
        bottomBar = {
            SelectionContainer {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .padding(WindowInsets.navigationBars.asPaddingValues()),
                    text = viewState.bottomLabel,
                    textAlign = TextAlign.Center,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScrollWithBar(),
        ) {
            SettingRow(title = "Daily targets", onTapped = onTargetsSettingTapped)
            SettingRow(title = "Customise AI", onTapped = onPromptEditorTapped)
            SettingRow(
                title = stringResource(R.string.settings_diary_day_start_row),
                subtitle = diaryDayStartSummary(viewState.diaryDayStartHour),
                onTapped = onDiaryDayStartTapped,
            )
            SettingRow(title = "Export summary JSON", onTapped = onExportSettingTapped)
            val backupIdle =
                !viewState.exportDataInProgress && !viewState.importDataInProgress
            SettingRow(
                title = "Export data",
                onTapped = onExportDbTapped,
                enabled = backupIdle,
                trailing = {
                    if (viewState.exportDataInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                },
            )
            SettingRow(
                title = "Import data (irreversible)",
                onTapped = onImportDbTapped,
                enabled = backupIdle,
                trailing = {
                    if (viewState.importDataInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                },
            )

            val isSignedIn = viewState.cloudSyncProvider != CloudSyncProvider.NONE
            val cloudSyncIdle = !viewState.cloudSyncInProgress
            SettingRow(
                title = "Cloud sync",
                subtitle = if (isSignedIn) viewState.cloudSyncEmail else "Sign in with Google",
                enabled = cloudSyncIdle,
                onTapped = onCloudSyncTapped,
                trailing = {
                    if (viewState.cloudSyncInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                },
            )
            if (isSignedIn) {
                val lastSyncedText = viewState.lastSyncedEpochMs?.let { ms ->
                    "Last synced: ${SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(ms))}"
                } ?: "Not synced yet"
                SettingRow(
                    title = "Sync now",
                    subtitle = lastSyncedText,
                    enabled = cloudSyncIdle,
                    onTapped = onSyncTapped,
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
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    onTapped: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            onExportSettingTapped = {},
            onExportDbTapped = {},
            onImportDbTapped = {},
            onCloudSyncTapped = {},
            onSyncTapped = {},
            onRestoreConfirmed = {},
            onRestoreDialogDismissed = {},
        )
    }
}
