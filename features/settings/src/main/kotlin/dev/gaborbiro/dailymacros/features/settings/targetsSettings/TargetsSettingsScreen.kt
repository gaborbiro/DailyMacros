package dev.gaborbiro.dailymacros.features.settings.targetsSettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetsSettingsUiUpdates
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.views.TargetsSettingsBottomSheet

@Composable
fun TargetsSettingsScreen(
    viewModel: TargetsSettingsViewModel,
    onCloseRequested: () -> Unit,
) {
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()

    TargetsSettingsBottomSheet(
        viewState = viewState,
        onDismissRequested = viewModel::onBottomSheetDismissRequested,
        onTargetChanged = viewModel::onTargetChanged,
        onResetTapped = viewModel::onTargetsResetTapped,
        onSaveTapped = viewModel::onSaveTapped,
        onUnsavedTargetsDiscardTapped = viewModel::onUnsavedTargetsDiscardTapped,
        onUnsavedTargetsDismissRequested = viewModel::onUnsavedTargetsDismissRequested,
    )

    LaunchedEffect(viewModel) {
        viewModel.uiUpdates.collect { event ->
            when (event) {
                TargetsSettingsUiUpdates.Hide, TargetsSettingsUiUpdates.Close -> onCloseRequested()
                else -> {}
            }
        }
    }
}
