package dev.gaborbiro.dailymacros.features.overview.views

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.res.Configuration
import android.util.Range
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Button
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.views.PreviewContext
import dev.gaborbiro.dailymacros.features.common.model.DailySummaryEntry
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelDailySummary
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelRecord
import dev.gaborbiro.dailymacros.features.common.model.MacrosAmountsUIModel
import dev.gaborbiro.dailymacros.features.overview.model.OverviewViewState
import dev.gaborbiro.dailymacros.features.widgetDiary.DiaryWidgetReceiver

@Composable
internal fun OverviewView(
    viewState: OverviewViewState,
    onRepeatMenuItemTapped: (recordId: Long) -> Unit,
    onAnalyseMacrosMenuItemTapped: (recordId: Long) -> Unit,
    onDetailsMenuItemTapped: (recordId: Long) -> Unit,
    onAddToQuickPicksMenuItemTapped: (recordId: Long) -> Unit,
    onDeleteMenuItemTapped: (recordId: Long) -> Unit,
    onRecordImageTapped: (recordId: Long) -> Unit,
    onRecordBodyTapped: (recordId: Long) -> Unit,
    onUndoDeleteTapped: () -> Unit,
    onUndoDeleteDismissed: () -> Unit,
    onUndoDeleteSnackbarShown: () -> Unit,
    onSearchTermChanged: (String?) -> Unit,
    onSettingsButtonTapped: () -> Unit,
    onTrendsButtonTapped: () -> Unit,
    onCoachMarkDismissed: () -> Unit,
    onLoadMore: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = viewState.showUndoDeleteSnackbar) {
        if (viewState.showUndoDeleteSnackbar) {
            val result = snackbarHostState.showSnackbar(
                message = "Record deleted",
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
            if (!viewState.showAddWidgetButton) {
                SearchFAB {
                    onSearchTermChanged(it)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
    ) { paddingValues ->
        if (viewState.items.isNotEmpty()) {
            OverviewList(
                viewState = viewState,
                paddingValues = paddingValues,
                onRepeatMenuItemTapped = onRepeatMenuItemTapped,
                onAnalyseMacrosMenuItemTapped = onAnalyseMacrosMenuItemTapped,
                onDetailsMenuItemTapped = onDetailsMenuItemTapped,
                onAddToQuickPicksMenuItemTapped = onAddToQuickPicksMenuItemTapped,
                onDeleteMenuItemTapped = onDeleteMenuItemTapped,
                onRecordImageTapped = onRecordImageTapped,
                onRecordBodyTapped = onRecordBodyTapped,
                onSettingsButtonTapped = onSettingsButtonTapped,
                onTrendsButtonTapped = onTrendsButtonTapped,
                onCoachMarkDismissed = onCoachMarkDismissed,
                onLoadMore = onLoadMore,
            )
        } else if (viewState.showAddWidgetButton) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                AddWidgetButton()
            }
        }
    }
}

@Composable
fun AddWidgetButton() {
    val context = LocalContext.current

    Button(
        modifier = Modifier
            .padding(PaddingDefault),
        onClick = {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            val widgetProvider = ComponentName(context, DiaryWidgetReceiver::class.java)

            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(widgetProvider, null, null)
            } else {
                Toast.makeText(context, "Pinning widgets is not supported on this launcher", Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Text(
            text = "Tap here to add a widget to your desktop"
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OverviewListPreview() {
    PreviewContext {
        OverviewView(
            viewState = OverviewViewState(
                items = listOf(
                    ListUiModelDailySummary(
                        listItemId = 1L,
                        dayTitle = "Yesterday",
                        entries = listOf(
                            DailySummaryEntry(
                                title = "Calories",
                                progress0to1 = .15f,
                                progressLabel = "1005kcal",
                                targetRange0to1 = Range(.84f, .88f),
                                targetRangeLabel = "2.1-2.2k",
                                color = { it.calorieColor },
                            ),
                            DailySummaryEntry(
                                title = "Protein",
                                progress0to1 = .0809f,
                                progressLabel = "110g",
                                targetRange0to1 = Range(.8095f, .9047f),
                                targetRangeLabel = "170-190g",
                                color = { it.proteinColor },
                            ),
                            DailySummaryEntry(
                                title = "Fat",
                                progress0to1 = .2121f,
                                progressLabel = "30g",
                                targetRange0to1 = Range(.6818f, .9091f),
                                targetRangeLabel = "45-60g",
                                color = { it.fatColor },
                            ),
                            DailySummaryEntry(
                                title = "Carbs",
                                progress0to1 = .1818f,
                                progressLabel = "105g",
                                targetRange0to1 = Range(.6818f, .9091f),
                                targetRangeLabel = "150-200g",
                                color = { it.carbsColor },
                            ),
                            DailySummaryEntry(
                                title = "Sugar",
                                progress0to1 = .2955f,
                                progressLabel = "35g",
                                targetRange0to1 = Range(.9091f, .9091f),
                                targetRangeLabel = "<40g/<25g added",
                                color = { it.carbsColor },
                            ),
                            DailySummaryEntry(
                                title = "Salt",
                                progress0to1 = .0f,
                                progressLabel = "0g",
                                targetRange0to1 = Range(.9091f, .9091f),
                                targetRangeLabel = "<5g (≈2g Na)",
                                color = { it.saltColor },
                            ),
                            DailySummaryEntry(
                                title = "Fibre",
                                progress0to1 = .0f,
                                progressLabel = "0g",
                                targetRange0to1 = Range(.9091f, .9091f),
                                targetRangeLabel = "30-38g",
                                color = { it.fibreColor },
                            ),
                        )
                    ),
                    ListUiModelRecord(
                        recordId = 2L,
                        title = "Title",
                        templateId = 2L,
                        images = listOf("", ""),
                        timestamp = "17:00",
                        macrosAmounts = MacrosAmountsUIModel(
                            calories = "8cal",
                            protein = "prot 8",
                            fat = "fat 4(2)",
                            carbs = "carb 9(9)",
                            salt = "sal 2",
                            fibre = "fib 4",
                        ),
                        showLoadingIndicator = false,
                        showAddToQuickPicksMenuItem = true,
                    ),
                    ListUiModelRecord(
                        recordId = 3L,
                        title = "Title 2",
                        templateId = 4L,
                        images = listOf("", ""),
                        timestamp = "15:38",
                        macrosAmounts = MacrosAmountsUIModel(
                            calories = "8cal",
                            protein = "prot 8",
                            fat = "fat 4(2)",
                            carbs = "carb 9(9)",
                            salt = "sal 2",
                            fibre = "fib 4",
                        ),
                        showLoadingIndicator = false,
                        showAddToQuickPicksMenuItem = true,
                    )
                ),
            ),
            onRepeatMenuItemTapped = {},
            onDetailsMenuItemTapped = {},
            onDeleteMenuItemTapped = {},
            onAddToQuickPicksMenuItemTapped = {},
            onRecordImageTapped = {},
            onRecordBodyTapped = {},
            onUndoDeleteTapped = {},
            onUndoDeleteDismissed = {},
            onUndoDeleteSnackbarShown = {},
            onSearchTermChanged = {},
            onAnalyseMacrosMenuItemTapped = {},
            onSettingsButtonTapped = {},
            onTrendsButtonTapped = {},
            onCoachMarkDismissed = {},
            onLoadMore = {},
        )
    }
}

@Preview(widthDp = 300)
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OverviewListPreviewEmpty() {
    PreviewContext {
        OverviewView(
            viewState = OverviewViewState(
                items = emptyList(),
                showAddWidgetButton = true,
            ),
            onRepeatMenuItemTapped = {},
            onDetailsMenuItemTapped = {},
            onDeleteMenuItemTapped = {},
            onAddToQuickPicksMenuItemTapped = {},
            onRecordImageTapped = {},
            onRecordBodyTapped = {},
            onUndoDeleteTapped = {},
            onUndoDeleteDismissed = {},
            onUndoDeleteSnackbarShown = {},
            onSearchTermChanged = {},
            onAnalyseMacrosMenuItemTapped = {},
            onSettingsButtonTapped = {},
            onTrendsButtonTapped = {},
            onCoachMarkDismissed = {},
            onLoadMore = {},
        )
    }
}
