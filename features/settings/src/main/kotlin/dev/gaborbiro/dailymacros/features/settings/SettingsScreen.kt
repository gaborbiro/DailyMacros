package dev.gaborbiro.dailymacros.features.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.gaborbiro.dailymacros.features.settings.export.rememberCreatePublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.export.rememberOpenPublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiUpdates
import dev.gaborbiro.dailymacros.features.settings.promptEditor.PromptEditorScreen
import dev.gaborbiro.dailymacros.features.settings.promptEditor.PromptEditorViewModel
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsScreen
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsViewModel
import dev.gaborbiro.dailymacros.features.settings.views.SettingsView

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    targetsSettingsViewModel: TargetsSettingsViewModel,
    promptEditorViewModel: PromptEditorViewModel,
    navController: NavHostController,
    highlightTargets: Boolean = false,
) {
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val createPublicDocumentUseCase = rememberCreatePublicDocumentUseCase()
    val openPublicDocumentUseCase = rememberOpenPublicDocumentUseCase()

    val photoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) settingsViewModel.onAutoPhotoPermissionsGranted()
        else settingsViewModel.onAutoPhotoPermissionsDenied()
    }

    LaunchedEffect(settingsViewModel) {
        settingsViewModel.uiUpdates.collect { event ->
            when (event) {
                SettingsUiUpdates.NavigateBack -> navController.popBackStack()
                is SettingsUiUpdates.ShowSnackbar -> snackbarHostState.showSnackbar(
                    event.message,
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite,
                )
                SettingsUiUpdates.RequestPhotoPermissions -> {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    photoPermissionLauncher.launch(permission)
                }
                else -> Unit
            }
        }
    }

    SettingsView(
        viewState = settingsUiState,
        snackbarHostState = snackbarHostState,
        highlightTargets = highlightTargets,
        onBackNavigateRequested = settingsViewModel::onBackNavigateRequested,
        onTargetsSettingTapped = settingsViewModel::onTargetsSettingsTapped,
        onPromptEditorTapped = settingsViewModel::onPromptEditorTapped,
        onDiaryDayStartTapped = settingsViewModel::onDiaryDayStartRowTapped,
        onDiaryDayStartDialogDismissed = settingsViewModel::onDiaryDayStartDialogDismissed,
        onDiaryDayStartHourSelected = settingsViewModel::onDiaryDayStartHourSelected,
        onAutoPhotoRecognitionToggled = settingsViewModel::onAutoPhotoRecognitionToggled,
        onQuickPickConfirmationToggled = settingsViewModel::onQuickPickConfirmationToggled,
        onExportSettingTapped = settingsViewModel::onExportSettingsTapped,
        onPdfExportDismissed = settingsViewModel::onPdfExportDialogDismissed,
        onPdfExportConfirmed = { selection, options ->
            settingsViewModel.onPdfExportConfirmed(createPublicDocumentUseCase, selection, options)
        },
        onExportDbTapped = { settingsViewModel.onExportDbTapped(createPublicDocumentUseCase) },
        onImportDbTapped = { settingsViewModel.onImportDbTapped(openPublicDocumentUseCase) },
        onCloudSyncTapped = settingsViewModel::onCloudSyncRowTapped,
        onSignOutConfirmed = settingsViewModel::onSignOutConfirmed,
        onSignOutDialogDismissed = settingsViewModel::onSignOutDialogDismissed,
        onSyncTapped = settingsViewModel::onSyncTapped,
        onRestoreFromDriveTapped = settingsViewModel::onRestoreFromDriveTapped,
        onRestoreConfirmed = settingsViewModel::onRestoreConfirmed,
        onRestoreDialogDismissed = settingsViewModel::onRestoreDialogDismissed,
        onAutoBackupIntervalTapped = settingsViewModel::onAutoBackupIntervalRowTapped,
        onAutoBackupIntervalSelected = settingsViewModel::onAutoBackupIntervalSelected,
        onAutoBackupIntervalDialogDismissed = settingsViewModel::onAutoBackupIntervalDialogDismissed,
        onOverwriteConfirmed = settingsViewModel::onOverwriteConfirmed,
        onOverwriteDialogDismissed = settingsViewModel::onOverwriteDialogDismissed,
    )

    if (settingsUiState.showTargetsSettings) {
        TargetsSettingsScreen(
            viewModel = targetsSettingsViewModel,
            onCloseRequested = settingsViewModel::onTargetsSettingsCloseRequested,
        )
    }

    if (settingsUiState.showPromptEditor) {
        PromptEditorScreen(
            viewModel = promptEditorViewModel,
            onCloseRequested = settingsViewModel::onPromptEditorCloseRequested,
        )
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
