package dev.gaborbiro.dailymacros.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun SettingsScreen(
    viewModel: SettingsViewModel,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    SettingsView(
        viewState = viewState,
        onBackClick = viewModel::onBackClick,
        onMacroTargetChange = viewModel::onMacroTargetChange,
        onReset = viewModel::reset
    )
}
