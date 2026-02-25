package dev.gaborbiro.dailymacros.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.settings.targets.TargetsSettingsRoute
import dev.gaborbiro.dailymacros.features.settings.targets.TargetsSettingsViewModel
import dev.gaborbiro.dailymacros.features.settings.views.SettingsView

@Composable
internal fun SettingsScreen(
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
        TargetsSettingsRoute(
            viewModel = targetsViewModel,
            onCloseRequested = settingsViewModel::onTargetsSettingsCloseRequested,
        )
    }
}
