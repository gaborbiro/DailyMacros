package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import android.util.Range
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
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.design.PaddingDouble
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressUIModel
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.overview.model.OverviewViewState
import dev.gaborbiro.dailymacros.util.randomBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
internal fun OverviewList(
    viewState: OverviewViewState,
    onRepeatMenuItemTapped: (RecordUIModel) -> Unit,
//    onChangeImageMenuItemTapped: (RecordUIModel) -> Unit,
//    onDeleteImageMenuItemTapped: (RecordUIModel) -> Unit,
    onEditRecordMenuItemTapped: (RecordUIModel) -> Unit,
    onDeleteRecordMenuItemTapped: (RecordUIModel) -> Unit,
    onMacrosMenuItemTapped: (RecordUIModel) -> Unit,
    onRecordImageTapped: (RecordUIModel) -> Unit,
    onRecordBodyTapped: (RecordUIModel) -> Unit,
    onUndoDeleteTapped: () -> Unit,
    onUndoDeleteDismissed: () -> Unit,
    onUndoDeleteSnackbarShown: () -> Unit,
    onSearchTermChanged: (String?) -> Unit,
) {
    val listState: LazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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
                .padding(top = paddingValues.calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(PaddingHalf),
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding() + 86.dp
            ),
            state = listState,
        ) {
            viewState.list.forEachIndexed { index, item ->
                when (item) {
                    is RecordUIModel -> {
                        item(key = item.recordId) {
                            RecordListItem(
                                modifier = Modifier
                                    .animateItem(),
                                record = item,
                                onRepeatMenuItemTapped = { record ->
                                    onRepeatMenuItemTapped(record)
                                    coroutineScope.launch {
                                        delay(200L)
                                        listState.scrollToItem(0)
                                    }
                                },
//                                onChangeImageMenuItemTapped = onChangeImageMenuItemTapped,
//                                onDeleteImageMenuItemTapped = onDeleteImageMenuItemTapped,
                                onEditRecordMenuItemTapped = onEditRecordMenuItemTapped,
                                onDeleteRecordMenuItemTapped = onDeleteRecordMenuItemTapped,
                                onMacrosMenuItemTapped = onMacrosMenuItemTapped,
                                onRecordImageTapped = onRecordImageTapped,
                                onRecordBodyTapped = onRecordBodyTapped,
                            )
                        }
                    }

                    is MacroProgressUIModel -> {
                        stickyHeader(key = item.date) {
                            MacroProgressView(
                                modifier = Modifier
                                    .let {
                                        if (index > 0) {
                                            it.padding(top = PaddingDouble)
                                        } else {
                                            it
                                        }
                                    },
                                model = item
                            )
                        }
                    }
                }
            }
        }
        ScrollToTopView(listState)
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NotesListPreview() {
    DailyMacrosTheme {
        OverviewList(
            viewState = OverviewViewState(
                list = listOf(
                    MacroProgressUIModel(
                        date = LocalDate.now(),
                        macros = listOf(
                            MacroProgressItem(
                                title = "Calories",
                                progress = .15f,
                                progressLabel = "1005 cal",
                                range = Range(.84f, .88f),
                                rangeLabel = "2.1-2.2kcal",
                            ),
                            MacroProgressItem(
                                title = "Protein",
                                progress = .0809f,
                                progressLabel = "110g",
                                range = Range(.8095f, .9047f),
                                rangeLabel = "170-190g",
                            ),
                            MacroProgressItem(
                                title = "Fat",
                                progress = .2121f,
                                progressLabel = "30g",
                                range = Range(.6818f, .9091f),
                                rangeLabel = "45-60g",
                            ),
                            MacroProgressItem(
                                title = "Carbs",
                                progress = .1818f,
                                progressLabel = "105g",
                                range = Range(.6818f, .9091f),
                                rangeLabel = "150-200g",
                            ),
                            MacroProgressItem(
                                title = "Sugar",
                                progress = .2955f,
                                progressLabel = "35g",
                                range = Range(.9091f, .9091f),
                                rangeLabel = "<40g ttl., <25g",
                            ),
                            MacroProgressItem(
                                title = "Salt",
                                progress = .0f,
                                progressLabel = "0g",
                                range = Range(.9091f, .9091f),
                                rangeLabel = "<5g (≈2g Na)",
                            ),
                            MacroProgressItem(
                                title = "Fibre",
                                progress = .0f,
                                progressLabel = "0g",
                                range = Range(.9091f, .9091f),
                                rangeLabel = "30-38g",
                            )
                        )
                    ),
                    RecordUIModel(
                        recordId = 1L,
                        title = "Title",
                        description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                        templateId = 1L,
                        images = emptyList(),
                        timestamp = "2022-01-01 00:00:00",
                        hasMacros = true,
                    ),
                    RecordUIModel(
                        recordId = 2L,
                        title = "Title 2",
                        description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                        templateId = 1L,
                        images = emptyList(),
                        timestamp = "2022-05-01 00:00:00",
                        hasMacros = true,
                    )
                ),
            ),
            onRepeatMenuItemTapped = {},
//            onChangeImageMenuItemTapped = {},
//            onDeleteImageMenuItemTapped = {},
            onEditRecordMenuItemTapped = {},
            onDeleteRecordMenuItemTapped = {},
            onRecordImageTapped = {},
            onRecordBodyTapped = {},
            onUndoDeleteTapped = {},
            onUndoDeleteDismissed = {},
            onUndoDeleteSnackbarShown = {},
            onSearchTermChanged = {},
            onMacrosMenuItemTapped = {},
        )
    }
}
