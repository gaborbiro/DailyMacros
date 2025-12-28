package dev.gaborbiro.dailymacros.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.settings.targets.TargetsSettingsRoute
import dev.gaborbiro.dailymacros.features.settings.targets.TargetsSettingsViewModel
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportFoodDiaryUseCase
import dev.gaborbiro.dailymacros.features.settings.views.SettingsView

@Composable
internal fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    targetsViewModel: TargetsSettingsViewModel,
) {
    val settingsViewState by settingsViewModel.viewState.collectAsStateWithLifecycle()

    SettingsView(
        viewState = settingsViewState,
        onBackNavigateRequested = settingsViewModel::onBackNavigateRequested,
        onTargetsSettingTapped = settingsViewModel::onTargetsSettingsTapped,
        onExportSettingTapped = settingsViewModel::onExportSettingsTapped,
    )

    if (settingsViewState.showTargetsSettings) {
        TargetsSettingsRoute(
            viewModel = targetsViewModel,
            onCloseRequested = settingsViewModel::onTargetsSettingsCloseRequested,
        )
    }
}
