package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import android.util.Range
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
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
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.overview.model.NutrientProgressItem
import dev.gaborbiro.dailymacros.features.overview.model.NutrientProgress
import dev.gaborbiro.dailymacros.features.overview.model.OverviewViewState
import dev.gaborbiro.dailymacros.util.randomBitmap
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
            verticalArrangement = Arrangement.spacedBy(PaddingHalf),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 86.dp
            ),
            state = listState,
        ) {
            viewState.todaysNutrientProgress?.let {
                item {
                    NutrientProgressView(it)
                }
            }
            viewState.yesterdaysNutrientProgress?.let {
                item {
                    NutrientProgressView(it)
                }
            }
            items(viewState.records.size, key = { viewState.records[it].recordId }) {
                OverviewListItem(
                    modifier = Modifier
                        .animateItem(),
                    record = viewState.records[it],
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
                        description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                        templateId = 1L,
                        bitmap = randomBitmap(),
                        timestamp = "2022-01-01 00:00:00"
                    ),
                    RecordUIModel(
                        recordId = 2L,
                        title = "Title 2",
                        description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                        templateId = 1L,
                        bitmap = randomBitmap(),
                        timestamp = "2022-05-01 00:00:00"
                    )
                ),
                todaysNutrientProgress = NutrientProgress(
                    calories = NutrientProgressItem(
                        title = "Calories",
                        progress = .15f,
                        progressLabel = "1005cal",
                        range = Range(.84f, .88f),
                        rangeLabel = "2.1-2.2kcal",
                    ),
                    protein = NutrientProgressItem(
                        title = "Protein",
                        progress = .0809f,
                        progressLabel = "110g",
                        range = Range(.8095f, .9047f),
                        rangeLabel = "170-190g",
                    ),
                    fat = NutrientProgressItem(
                        title = "Fat",
                        progress = .2121f,
                        progressLabel = "30g",
                        range = Range(.6818f, .9091f),
                        rangeLabel = "45-60g",
                    ),
                    carbs = NutrientProgressItem(
                        title = "Carbs",
                        progress = .1818f,
                        progressLabel = "105g",
                        range = Range(.6818f, .9091f),
                        rangeLabel = "150-200g",
                    ),
                    sugar = NutrientProgressItem(
                        title = "Sugar",
                        progress = .2955f,
                        progressLabel = "35g",
                        range = Range(.9091f, .9091f),
                        rangeLabel = "<40g ttl., <25g",
                    ),
                    salt = NutrientProgressItem(
                        title = "Salt",
                        progress = .0f,
                        progressLabel = "0g",
                        range = Range(.9091f, .9091f),
                        rangeLabel = "<5g (≈2g Na)",
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
