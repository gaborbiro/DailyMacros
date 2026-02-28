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
    viewModel: TargetsSettingsViewModel,
    onCloseRequested: () -> Unit,
) {
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()
    val events = viewModel.uiEvents

    TargetsSettingsBottomSheet(
        viewState = viewState,
        events = events,
        onDismissRequested = onCloseRequested,
        onTargetChanged = viewModel::onTargetChanged,
        onResetTapped = viewModel::onTargetsResetTapped,
        onSaveTapped = viewModel::onSaveTapped,
        onUnsavedTargetsDiscardTapped = viewModel::onUnsavedTargetsDiscardTapped,
        onUnsavedTargetsDismissRequested = viewModel::onUnsavedTargetsDismissRequested,
    )

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                TargetsSettingsUiEvents.Close -> onCloseRequested()
                else -> { }
            }
        }
    }
}
