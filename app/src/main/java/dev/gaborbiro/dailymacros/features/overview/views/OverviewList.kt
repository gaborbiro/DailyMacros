package dev.gaborbiro.dailymacros.features.overview.views

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
import dev.gaborbiro.dailymacros.design.NotesTheme
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.model.RecordViewState
import dev.gaborbiro.dailymacros.features.overview.model.OverviewViewState
import dev.gaborbiro.dailymacros.util.dummyBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun OverviewList(
    viewState: OverviewViewState,
    onRepeatMenuItemTapped: (RecordViewState) -> Unit,
    onChangeImageMenuItemTapped: (RecordViewState) -> Unit,
    onDeleteImageMenuItemTapped: (RecordViewState) -> Unit,
    onEditRecordMenuItemTapped: (RecordViewState) -> Unit,
    onDeleteRecordMenuItemTapped: (RecordViewState) -> Unit,
    onNutrientsMenuItemTapped: (RecordViewState) -> Unit,
    onRecordImageTapped: (RecordViewState) -> Unit,
    onRecordBodyTapped: (RecordViewState) -> Unit,
    onUndoDeleteTapped: () -> Unit,
    onUndoDeleteDismissed: () -> Unit,
    onUndoDeleteSnackbarShown: () -> Unit,
    onSearchTermChanged: (String?) -> Unit,
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
                SnackbarResult.ActionPerformed -> onUndoDeleteTapped()
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
                OverviewListItem(
                    modifier = Modifier.Companion
                        .animateItem(),
                    record = records[it],
                    onRepeatMenuItemTapped = { record ->
                        onRepeatMenuItemTapped(record)
                        coroutineScope.launch {
                            delay(200L)
                            listState.scrollToItem(0)
                        }
                    },
                    onChangeImageMenuItemTapped = onChangeImageMenuItemTapped,
                    onDeleteImageMenuItemTapped = onDeleteImageMenuItemTapped,
                    onEditRecordMenuItemTapped = onEditRecordMenuItemTapped,
                    onDeleteRecordMenuItemTapped = onDeleteRecordMenuItemTapped,
                    onNutrientsMenuItemTapped = onNutrientsMenuItemTapped,
                    onRecordImageTapped = onRecordImageTapped,
                    onRecordBodyTapped = onRecordBodyTapped,
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
        OverviewList(
            viewState = OverviewViewState(
                records = listOf(
                    RecordViewState(
                        recordId = 1L,
                        title = "Title",
                        templateId = 1L,
                        bitmap = dummyBitmap(),
                        timestamp = "2022-01-01 00:00:00"
                    ),
                    RecordViewState(
                        recordId = 2L,
                        title = "Title 2",
                        templateId = 1L,
                        bitmap = dummyBitmap(),
                        timestamp = "2022-05-01 00:00:00"
                    )
                )
            ),
            onRepeatMenuItemTapped = {},
            onChangeImageMenuItemTapped = {},
            onDeleteImageMenuItemTapped = {},
            onEditRecordMenuItemTapped = {},
            onDeleteRecordMenuItemTapped = {},
            onRecordImageTapped = {},
            onRecordBodyTapped = {},
            onUndoDeleteTapped = {},
            onUndoDeleteDismissed = {},
            onUndoDeleteSnackbarShown = {},
            onSearchTermChanged = {},
            onNutrientsMenuItemTapped = {},
        )
    }
}
