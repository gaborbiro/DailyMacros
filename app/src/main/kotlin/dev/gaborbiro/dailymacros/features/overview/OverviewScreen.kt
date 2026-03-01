package dev.gaborbiro.dailymacros.features.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.gaborbiro.dailymacros.features.main.SETTINGS_ROUTE
import dev.gaborbiro.dailymacros.features.main.TRENDS_ROUTE
import dev.gaborbiro.dailymacros.features.modal.ModalActivity
import dev.gaborbiro.dailymacros.features.overview.model.OverviewUiUpdates
import dev.gaborbiro.dailymacros.features.overview.views.OverviewView

@Composable
internal fun OverviewScreen(
    viewModel: OverviewViewModel,
    navController: NavHostController,
) {
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.uiUpdates.collect { event ->
            when (event) {
                is OverviewUiUpdates.EditRecord -> ModalActivity.launchViewRecordDetails(context, event.recordId)
                is OverviewUiUpdates.ViewImage -> ModalActivity.launchToShowRecordImage(context, event.recordId)
                OverviewUiUpdates.OpenSettingsScreen -> navController.navigate(SETTINGS_ROUTE)
                OverviewUiUpdates.OpenTrendsScreen -> navController.navigate(TRENDS_ROUTE)
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.onSearchTermChanged(search = null)
    }

    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    OverviewView(
        viewState = viewState,
        onRepeatMenuItemTapped = viewModel::onRepeatMenuItemTapped,
        onAnalyseMacrosMenuItemTapped = viewModel::onAnalyseMacrosMenuItemTapped,
        onDetailsMenuItemTapped = viewModel::onDetailsMenuItemTapped,
        onAddToQuickPicksMenuItemTapped = viewModel::onAddToQuickPicksMenuItemTapped,
        onDeleteMenuItemTapped = viewModel::onDeleteMenuItemTapped,
        onRecordImageTapped = viewModel::onRecordImageTapped,
        onRecordBodyTapped = viewModel::onRecordBodyTapped,
        onUndoDeleteTapped = viewModel::onUndoDeleteTapped,
        onUndoDeleteDismissed = viewModel::onUndoDeleteDismissed,
        onUndoDeleteSnackbarShown = viewModel::onUndoDeleteSnackbarShown,
        onSearchTermChanged = viewModel::onSearchTermChanged,
        onSettingsButtonTapped = viewModel::onSettingsButtonTapped,
        onTrendsButtonTapped = viewModel::onTrendsButtonTapped,
        onCoachMarkDismissed = viewModel::onCoachMarkDismissed,
        onLoadMore = viewModel::onLoadMore,
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
