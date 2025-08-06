@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package dev.gaborbiro.nutri.features.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.nutri.features.notes.views.NotesList
import dev.gaborbiro.nutri.features.widget.NotesWidget

@Composable
fun NotesScreen(
    viewModel: NotesScreenViewModel,
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

    NotesList(
        viewState,
        onDuplicateRecord = viewModel::onDuplicateRecordRequested,
        onUpdateImage = viewModel::onUpdateImageRequested,
        onDeleteImage = viewModel::onDeleteImageRequested,
        onEditRecord = viewModel::onEditRecordRequested,
        onDeleteRecord = viewModel::onDeleteRecordRequested,
        onImageTapped = viewModel::onImageTapped,
        onUndoDelete = viewModel::onUndoDeleteRequested,
        onUndoDeleteDismissed = viewModel::onUndoDeleteDismissed,
        onUndoDeleteSnackbarShown = viewModel::onUndoDeleteSnackbarShown,
        onSearchTermChanged = viewModel::onSearchTermChanged,
        onNutrientsRequested = viewModel::onNutrientsRequested,
    )
}
