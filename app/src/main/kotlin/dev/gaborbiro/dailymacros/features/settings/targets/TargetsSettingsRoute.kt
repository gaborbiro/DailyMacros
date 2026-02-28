package dev.gaborbiro.dailymacros.features.settings.targets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.settings.targets.model.TargetsSettingsUiEvents
import dev.gaborbiro.dailymacros.features.settings.targets.views.TargetsSettingsBottomSheet
import kotlinx.coroutines.flow.Flow

@Composable
internal fun TargetsSettingsRoute(
    onCloseRequested: () -> Unit,
    viewModel: TargetsSettingsViewModel
) {
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                TargetsSettingsUiEvents.Close -> onCloseRequested()
                TargetsSettingsUiEvents.Hide, TargetsSettingsUiEvents.Show -> {
                    // handled in TargetsBottomSheet
                }
            }
        }
    }
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()
    val events: Flow<TargetsSettingsUiEvents> = viewModel.uiEvents

    TargetsSettingsBottomSheet(
        viewState = viewState,
        events = events,
        onDismissRequested = viewModel::onBottomSheetDismissRequested,
        onTargetChanged = viewModel::onTargetChanged,
        onResetTapped = viewModel::onTargetsResetTapped,
        onSaveTapped = viewModel::onSaveTapped,
        onUnsavedTargetsDiscardTapped = viewModel::onUnsavedTargetsDiscardTapped,
        onUnsavedTargetsDismissRequested = viewModel::onUnsavedTargetsDismissRequested,
    )
}