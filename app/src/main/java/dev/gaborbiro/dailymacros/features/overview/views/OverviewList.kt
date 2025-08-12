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
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.overview.model.GoalCellItem
import dev.gaborbiro.dailymacros.features.overview.model.MacroGoalsProgress
import dev.gaborbiro.dailymacros.features.overview.model.OverviewViewState
import dev.gaborbiro.dailymacros.util.dummyBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun OverviewList(
    viewState: OverviewViewState,
    onRepeatMenuItemTapped: (RecordUIModel) -> Unit,
    onChangeImageMenuItemTapped: (RecordUIModel) -> Unit,
    onDeleteImageMenuItemTapped: (RecordUIModel) -> Unit,
    onEditRecordMenuItemTapped: (RecordUIModel) -> Unit,
    onDeleteRecordMenuItemTapped: (RecordUIModel) -> Unit,
    onNutrientsMenuItemTapped: (RecordUIModel) -> Unit,
    onRecordImageTapped: (RecordUIModel) -> Unit,
    onRecordBodyTapped: (RecordUIModel) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(PaddingDefault),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 86.dp
            ),
            state = listState,
        ) {
            viewState.macroGoalsProgress?.let {
                item {
                    MacroGoalsView(it)
                }
            }
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
    DailyMacrosTheme {
        OverviewList(
            viewState = OverviewViewState(
                records = listOf(
                    RecordUIModel(
                        recordId = 1L,
                        title = "Title",
                        templateId = 1L,
                        bitmap = dummyBitmap(),
                        timestamp = "2022-01-01 00:00:00"
                    ),
                    RecordUIModel(
                        recordId = 2L,
                        title = "Title 2",
                        templateId = 1L,
                        bitmap = dummyBitmap(),
                        timestamp = "2022-05-01 00:00:00"
                    )
                ),
                macroGoalsProgress = MacroGoalsProgress(
                    calories = GoalCellItem(
                        title = "Calories",
                        value = "1005 cal",
                        rangeLabel = "2.1-2.2kcal",
                        range = Range(.84f, .88f),
                        progress = .15f,
                    ),
                    protein = GoalCellItem(
                        title = "Protein",
                        value = "110g",
                        rangeLabel = "170-190g",
                        range = Range(.8095f, .9047f),
                        progress = .0809f,
                    ),
                    fat = GoalCellItem(
                        title = "Fat",
                        value = "30g",
                        rangeLabel = "45-60g",
                        range = Range(.6818f, .9091f),
                        progress = .2121f,
                    ),
                    carbs = GoalCellItem(
                        title = "Carbs",
                        value = "105g",
                        rangeLabel = "150-200g",
                        range = Range(.6818f, .9091f),
                        progress = .1818f,
                    ),
                    sugar = GoalCellItem(
                        title = "Sugar",
                        value = "35g",
                        rangeLabel = "<40g ttl., <25g",
                        range = Range(.9091f, .9091f),
                        progress = .2955f,
                    ),
                    salt = GoalCellItem(
                        title = "Salt",
                        value = "0g",
                        rangeLabel = "<5g (≈2g Na)",
                        range = Range(.9091f, .9091f),
                        progress = .0f,
                    ),
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
