package dev.gaborbiro.dailymacros.features.settings.targets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.settings.targets.model.TargetsSettingsEvents
import dev.gaborbiro.dailymacros.features.settings.targets.views.TargetsSettingsBottomSheet
import kotlinx.coroutines.flow.Flow

@Composable
internal fun TargetsSettingsRoute(
    onCloseRequested: () -> Unit,
    viewModel: TargetsSettingsViewModel
) {
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TargetsSettingsEvents.Close -> onCloseRequested()
                TargetsSettingsEvents.Hide, TargetsSettingsEvents.Show -> {
                    // handled in TargetsBottomSheet
                }
            }
        }
    }
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val events: Flow<TargetsSettingsEvents> = viewModel.events

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