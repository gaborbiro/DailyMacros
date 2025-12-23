package dev.gaborbiro.dailymacros.features.settings.targets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.settings.targets.model.TargetsEvents
import dev.gaborbiro.dailymacros.features.settings.targets.views.TargetsBottomSheet
import kotlinx.coroutines.flow.Flow

@Composable
internal fun TargetsSettingsRoute(
    onCloseRequested: () -> Unit,
    viewModel: TargetsSettingsViewModel
) {
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TargetsEvents.Close -> onCloseRequested()
                TargetsEvents.Hide, TargetsEvents.Show -> {
                    // handled in TargetsBottomSheet
                }
            }
        }
    }
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val events: Flow<TargetsEvents> = viewModel.events

    TargetsBottomSheet(
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