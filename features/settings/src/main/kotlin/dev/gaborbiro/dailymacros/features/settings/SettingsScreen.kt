package dev.gaborbiro.dailymacros.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsScreen
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsViewModel
import dev.gaborbiro.dailymacros.features.settings.views.SettingsView

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    targetsViewModel: TargetsSettingsViewModel,
) {
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    SettingsView(
        viewState = settingsUiState,
        onBackNavigateRequested = settingsViewModel::onBackNavigateRequested,
        onTargetsSettingTapped = settingsViewModel::onTargetsSettingsTapped,
        onExportSettingTapped = settingsViewModel::onExportSettingsTapped,
    )

    if (settingsUiState.showTargetsSettings) {
        TargetsSettingsScreen(
            viewModel = targetsViewModel,
            onCloseRequested = settingsViewModel::onTargetsSettingsCloseRequested,
        )
    }
}
