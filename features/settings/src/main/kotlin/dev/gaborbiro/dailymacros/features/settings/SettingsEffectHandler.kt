package dev.gaborbiro.dailymacros.features.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.gaborbiro.dailymacros.features.settings.export.ProcessRestarter
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiUpdates

// Mounted once at the app root regardless of the current screen (including onboarding,
// which has no Scaffold/snackbar of its own), so this is the single place cloud
// sync/restore feedback surfaces.
@Composable
fun SettingsEffectHandler(
    settingsViewModel: SettingsViewModel,
    onRestartApplication: () -> Unit = {},
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val signInLauncher = rememberGoogleSignInLauncher(
        onSuccess = settingsViewModel::onGoogleSignInSuccess,
        onFailure = settingsViewModel::onGoogleSignInFailed,
    )

    var restoreConfirmEvent by remember {
        mutableStateOf<SettingsUiUpdates.RestoreConfirmNeeded?>(null)
    }

    LaunchedEffect(settingsViewModel) {
        settingsViewModel.uiUpdates.collect { event ->
            when (event) {
                SettingsUiUpdates.RequestGoogleSignIn ->
                    launchGoogleSignIn(context, signInLauncher)
                SettingsUiUpdates.RestartApplication -> {
                    onRestartApplication()
                    ProcessRestarter.restartApplication(context.findActivity()!!)
                }
                is SettingsUiUpdates.RestoreConfirmNeeded ->
                    restoreConfirmEvent = event
                is SettingsUiUpdates.ShowSnackbar ->
                    snackbarHostState.showSnackbar(
                        event.message,
                        withDismissAction = true,
                        duration = SnackbarDuration.Indefinite,
                    )
                else -> Unit
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    restoreConfirmEvent?.let { event ->
        val dateStr = remember(event.modifiedAtMs) {
            java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(event.modifiedAtMs))
        }
        AlertDialog(
            onDismissRequest = { restoreConfirmEvent = null },
            title = { Text(stringResource(R.string.settings_dialog_restore_title)) },
            text = { Text(stringResource(R.string.settings_dialog_restore_message, dateStr)) },
            confirmButton = {
                TextButton(onClick = {
                    restoreConfirmEvent = null
                    settingsViewModel.onRestoreConfirmed()
                }) { Text(stringResource(R.string.settings_dialog_restore_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { restoreConfirmEvent = null }) {
                    Text(stringResource(R.string.settings_dialog_cancel))
                }
            },
        )
    }
}
