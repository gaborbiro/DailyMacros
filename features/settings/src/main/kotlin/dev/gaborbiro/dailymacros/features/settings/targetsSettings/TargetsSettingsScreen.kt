package dev.gaborbiro.dailymacros.features.settings.targetsSettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetsSettingsUiEvents
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.views.TargetsSettingsBottomSheet

@Composable
internal fun TargetsSettingsScreen(
    viewModel: TargetsSettingsViewModel,
    onCloseRequested: () -> Unit,
) {
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()

    TargetsSettingsBottomSheet(
        viewState = viewState,
        events = viewModel.uiEvents,
        onDismissRequested = onCloseRequested,
        onTargetChanged = viewModel::onTargetChanged,
        onResetTapped = viewModel::onTargetsResetTapped,
        onSaveTapped = viewModel::onSaveTapped,
        onUnsavedTargetsDiscardTapped = viewModel::onUnsavedTargetsDiscardTapped,
        onUnsavedTargetsDismissRequested = viewModel::onUnsavedTargetsDismissRequested,
    )

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                TargetsSettingsUiEvents.Close -> onCloseRequested()
                else -> {
                    // nothing to do
                }
            }
        }
    }
}
