package dev.gaborbiro.dailymacros.features.settings

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiUpdates
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsScreen
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsViewModel
import dev.gaborbiro.dailymacros.features.settings.views.SettingsView

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    targetsSettingsViewModel: TargetsSettingsViewModel,
    navController: NavHostController,
    showLineageRetrofillButton: Boolean = false,
) {
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(settingsViewModel) {
        settingsViewModel.uiUpdates.collect { event ->
            when (event) {
                SettingsUiUpdates.NavigateBack -> navController.popBackStack()
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
        onExportSettingTapped = settingsViewModel::onExportSettingsTapped,
        onExportDbTapped = settingsViewModel::onExportDbTapped,
        onImportDbTapped = settingsViewModel::onImportDbTapped,
        onVariabilityMiningPreviewTapped = settingsViewModel::onVariabilityMiningPreviewTapped,
        onClearVariabilityProfileTapped = settingsViewModel::onClearVariabilityProfileTapped,
        onCopyVariabilityRequestJson = settingsViewModel::onCopyVariabilityRequestJson,
        onCopyVariabilityResponseJson = settingsViewModel::onCopyVariabilityResponseJson,
        onVariabilityMiningRequestJsonExpansionBitsChange =
            settingsViewModel::onVariabilityMiningRequestJsonExpansionBitsChange,
        onVariabilityMiningResponseJsonExpansionBitsChange =
            settingsViewModel::onVariabilityMiningResponseJsonExpansionBitsChange,
        onVariabilityMiningRequestJsonSectionExpandedChange =
            settingsViewModel::onVariabilityMiningRequestJsonSectionExpandedChange,
        onVariabilityMiningResponseJsonSectionExpandedChange =
            settingsViewModel::onVariabilityMiningResponseJsonSectionExpandedChange,
        showLineageRetrofillButton = showLineageRetrofillButton,
        onRetrofillParentLineageTapped = settingsViewModel::onRetrofillParentLineageTapped,
    )

    if (settingsUiState.showTargetsSettings) {
        TargetsSettingsScreen(
            viewModel = targetsSettingsViewModel,
            onCloseRequested = settingsViewModel::onTargetsSettingsCloseRequested,
        )
    }
}
