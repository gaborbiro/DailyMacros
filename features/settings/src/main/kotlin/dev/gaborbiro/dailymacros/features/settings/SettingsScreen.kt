package dev.gaborbiro.dailymacros.features.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.gaborbiro.dailymacros.features.settings.export.ProcessRestarter
import dev.gaborbiro.dailymacros.features.settings.export.rememberCreatePublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.export.rememberOpenPublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiUpdates
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsScreen
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsViewModel
import dev.gaborbiro.dailymacros.features.settings.views.SettingsView

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    targetsSettingsViewModel: TargetsSettingsViewModel,
    navController: NavHostController,
) {
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val createPublicDocumentUseCase = rememberCreatePublicDocumentUseCase()
    val openPublicDocumentUseCase = rememberOpenPublicDocumentUseCase()

    LaunchedEffect(settingsViewModel, context) {
        settingsViewModel.uiUpdates.collect { event ->
            when (event) {
                SettingsUiUpdates.NavigateBack -> navController.popBackStack()
                SettingsUiUpdates.RestartApplication -> context.findActivity()?.let {
                    ProcessRestarter.restartApplication(it)
                }
                is SettingsUiUpdates.ShowSnackbar -> snackbarHostState.showSnackbar(
                    event.message,
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite,
                )
            }
        }
    }

    SettingsView(
        viewState = settingsUiState,
        snackbarHostState = snackbarHostState,
        onBackNavigateRequested = settingsViewModel::onBackNavigateRequested,
        onTargetsSettingTapped = settingsViewModel::onTargetsSettingsTapped,
        onExportSettingTapped = { settingsViewModel.onExportSettingsTapped(createPublicDocumentUseCase) },
        onExportDbTapped = { settingsViewModel.onExportDbTapped(createPublicDocumentUseCase) },
        onImportDbTapped = { settingsViewModel.onImportDbTapped(openPublicDocumentUseCase) },
    )

    if (settingsUiState.showTargetsSettings) {
        TargetsSettingsScreen(
            viewModel = targetsSettingsViewModel,
            onCloseRequested = settingsViewModel::onTargetsSettingsCloseRequested,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
