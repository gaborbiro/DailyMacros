@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package dev.gaborbiro.dailymacros.features.overview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.overview.views.OverviewList
import dev.gaborbiro.dailymacros.features.widget.NotesWidget

@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        viewModel.onSearchTermChanged(search = null)
    }

    val viewState by viewModel.uiState.collectAsStateWithLifecycle()

    if (viewState.refreshWidget) {
        viewModel.onWidgetRefreshed()
        NotesWidget.reload(context)
    }

    OverviewList(
        viewState,
        onRepeatMenuItemTapped = viewModel::onRepeatMenuItemTapped,
        onChangeImageMenuItemTapped = viewModel::onChangeImageMenuItemTapped,
        onDeleteImageMenuItemTapped = viewModel::onDeleteImageMenuItemTapped,
        onEditRecordMenuItemTapped = viewModel::onEditRecordMenuItemTapped,
        onDeleteRecordMenuItemTapped = viewModel::onDeleteRecordMenuItemTapped,
        onNutrientsMenuItemTapped = viewModel::onNutrientsMenuItemTapped,
        onRecordImageTapped = viewModel::onRecordImageTapped,
        onRecordBodyTapped = viewModel::onRecordBodyTapped,
        onUndoDeleteTapped = viewModel::onUndoDeleteTapped,
        onUndoDeleteDismissed = viewModel::onUndoDeleteDismissed,
        onUndoDeleteSnackbarShown = viewModel::onUndoDeleteSnackbarShown,
        onSearchTermChanged = viewModel::onSearchTermChanged,
    )
}
