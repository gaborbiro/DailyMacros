package dev.gaborbiro.dailymacros.features.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.overview.views.OverviewView

@Composable
internal fun OverviewScreen(
    viewModel: OverviewViewModel,
) {
    LaunchedEffect(key1 = Unit) {
        viewModel.onSearchTermChanged(search = null)
    }

    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    OverviewView(
        viewState = viewState,
        onRepeatMenuItemTapped = viewModel::onRepeatRecordMenuItemTapped,
        onDetailsMenuItemTapped = viewModel::onDetailsMenuItemTapped,
        onDeleteRecordMenuItemTapped = viewModel::onDeleteRecordMenuItemTapped,
        onMacrosMenuItemTapped = viewModel::onMacrosMenuItemTapped,
        onRecordImageTapped = viewModel::onRecordImageTapped,
        onRecordBodyTapped = viewModel::onRecordBodyTapped,
        onUndoDeleteTapped = viewModel::onUndoDeleteTapped,
        onUndoDeleteDismissed = viewModel::onUndoDeleteDismissed,
        onUndoDeleteSnackbarShown = viewModel::onUndoDeleteSnackbarShown,
        onSearchTermChanged = viewModel::onSearchTermChanged,
        onSettingsButtonTapped = viewModel::onSettingsButtonTapped,
        onCoachMarkDismissed = viewModel::onCoachMarkDismissed,
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.finalizePendingUndos()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
