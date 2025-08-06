package dev.gaborbiro.nutri.features.notes.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.nutri.design.NotesTheme
import dev.gaborbiro.nutri.design.PaddingDefault
import dev.gaborbiro.nutri.features.common.model.RecordViewState
import dev.gaborbiro.nutri.features.notes.model.NotesViewState
import dev.gaborbiro.nutri.util.createDummyBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun NotesList(
    viewState: NotesViewState,
    onDuplicateRecord: (RecordViewState) -> Unit,
    onUpdateImage: (RecordViewState) -> Unit,
    onDeleteImage: (RecordViewState) -> Unit,
    onEditRecord: (RecordViewState) -> Unit,
    onDeleteRecord: (RecordViewState) -> Unit,
    onImageTapped: (RecordViewState) -> Unit,
    onUndoDelete: () -> Unit,
    onUndoDeleteDismissed: () -> Unit,
    onUndoDeleteSnackbarShown: () -> Unit,
    onSearchTermChanged: (String?) -> Unit,
    onNutrientsRequested: (RecordViewState) -> Unit,
) {
    val listState: LazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val records = viewState.records

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = viewState.showUndoDeleteSnackbar) {
        if (viewState.showUndoDeleteSnackbar) {
            val result = snackbarHostState.showSnackbar(
                message = "Note deleted",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> onUndoDelete()
                SnackbarResult.Dismissed -> onUndoDeleteDismissed()
            }
            onUndoDeleteSnackbarShown()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        floatingActionButton = {
            SearchFAB {
                onSearchTermChanged(it)
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(PaddingDefault),
            contentPadding = PaddingValues(top = PaddingDefault, bottom = 86.dp),
            state = listState,
        ) {
            items(records.size, key = { records[it].recordId }) {
                NoteListItem(
                    modifier = Modifier.Companion
                        .animateItem(),
                    record = records[it],
                    onDuplicateRecord = { record ->
                        onDuplicateRecord(record)
                        coroutineScope.launch {
                            delay(200L)
                            listState.scrollToItem(0)
                        }
                    },
                    onUpdateImage = onUpdateImage,
                    onDeleteImage = onDeleteImage,
                    onEditRecord = onEditRecord,
                    onDeleteRecord = onDeleteRecord,
                    onImageTapped = onImageTapped,
                    onNutrientsRequested = onNutrientsRequested,
                )
            }
        }
        ScrollToTopView(listState)
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NotesListPreview() {
    NotesTheme {
        NotesList(
            viewState = NotesViewState(
                records = listOf(
                    RecordViewState(
                        recordId = 1L,
                        title = "Title",
                        templateId = 1L,
                        bitmap = createDummyBitmap(),
                        timestamp = "2022-01-01 00:00:00"
                    ),
                    RecordViewState(
                        recordId = 2L,
                        title = "Title 2",
                        templateId = 1L,
                        bitmap = createDummyBitmap(),
                        timestamp = "2022-05-01 00:00:00"
                    )
                )
            ),
            onDuplicateRecord = {},
            onUpdateImage = {},
            onDeleteImage = {},
            onEditRecord = {},
            onDeleteRecord = {},
            onImageTapped = {},
            onUndoDelete = {},
            onUndoDeleteDismissed = {},
            onUndoDeleteSnackbarShown = {},
            onSearchTermChanged = {},
            onNutrientsRequested = {},
        )
    }
}
