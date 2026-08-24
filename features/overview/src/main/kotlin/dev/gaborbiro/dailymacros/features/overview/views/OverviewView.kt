package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import android.util.Range
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.features.overview.R
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.overview.model.DailySummaryEntry
import dev.gaborbiro.dailymacros.features.overview.model.ListUiModelDailySummary
import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelRecord
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.features.common.views.PreviewContext
import dev.gaborbiro.dailymacros.features.overview.model.OverviewUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
internal fun OverviewView(
    viewState: OverviewUiState,
    onRepeatMenuItemTapped: (recordId: Long) -> Unit,
    onAnalyseMacrosMenuItemTapped: (recordId: Long) -> Unit,
    onDeleteMenuItemTapped: (recordId: Long) -> Unit,
    onRecordImageTapped: (recordId: Long) -> Unit,
    onRecordBodyTapped: (recordId: Long) -> Unit,
    onUndoDeleteTapped: () -> Unit,
    onUndoDeleteDismissed: () -> Unit,
    onUndoDeleteSnackbarShown: () -> Unit,
    onSearchTermChanged: (String?) -> Unit,
    onSettingsButtonTapped: () -> Unit,
    onDailySummaryTapped: (epochDay: Long) -> Unit,
    onWeeklySummaryTapped: (epochDay: Long) -> Unit,
    onSubscribeBannerTapped: () -> Unit = {},
    onSubscribeBannerDismissed: () -> Unit = {},
    onAddWidget: () -> Unit = {},
    onRestoreFromCloud: () -> Unit = {},
    restoreFromCloudInProgress: Boolean = false,
    onRestoreFromLocalBackup: () -> Unit = {},
    restoreFromLocalBackupInProgress: Boolean = false,
    onLoadMore: () -> Unit = {},
    onScrollHandled: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val recordDeletedMessage = stringResource(R.string.overview_content_record_deleted)
    val undoLabel = stringResource(R.string.overview_content_undo)

    LaunchedEffect(key1 = viewState.showUndoDeleteSnackbar) {
        if (viewState.showUndoDeleteSnackbar) {
            val result = snackbarHostState.showSnackbar(
                message = recordDeletedMessage,
                actionLabel = undoLabel,
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

    val listState = rememberLazyListState()

    // Slide the FABs out while the user scrolls towards the past (further down the reversed
    // list, i.e. "forward" through it) and back in as soon as they scroll back towards the top
    // ("backward"), even a little. lastScrolledForward/Backward are ScrollableState's own
    // direction tracking, sticky until the next scroll in the other direction - exactly what's
    // needed here, and driven by the list's actual scroll gestures rather than
    // listState.firstVisibleItemIndex, whose "am I at the top" check an earlier version of this
    // relied on: that could get permanently stuck if a new record prepended while backgrounded
    // shifted Compose's scroll-anchoring so index 0 never reported "true" again (see
    // OverviewListTopActions.kt history). A hand-rolled NestedScrollConnection reading raw
    // pre-scroll deltas was tried here too and didn't reliably react to scrolling at all -
    // this is the API Compose itself maintains for this exact "hide on scroll" pattern.
    val fabsVisible by remember { derivedStateOf { !listState.lastScrolledForward } }

    // A tap on a Trends chart point resolves to a day already loaded here (see
    // OverviewViewModel.onScrollToDateRequested) and is surfaced as a one-shot listItemId to
    // scroll to; once acted on, onScrollHandled() clears it so it doesn't fire again on an
    // unrelated recomposition (e.g. a new record arriving).
    LaunchedEffect(viewState.scrollToListItemId, viewState.items) {
        val targetId = viewState.scrollToListItemId ?: return@LaunchedEffect
        val index = viewState.items.indexOfFirst { it.listItemId == targetId }
        if (index >= 0) {
            listState.scrollToItem(index)
            onScrollHandled()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
    ) { paddingValues ->
        // The top system-bar inset is consumed once here (Column), so the banner sits below
        // the status bar and the Box below starts at y=0 of its own bounds - remainingPadding
        // passes only the bottom inset onward, and OverviewListTopActions gets topContentPadding
        // = 0 since it no longer needs to skip a status bar it's not adjacent to.
        val remainingPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            if (viewState.showSubscribeBanner) {
                SubscribeBanner(
                    onSubscribeTapped = onSubscribeBannerTapped,
                    onDismissed = onSubscribeBannerDismissed,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f),
            ) {
                if (viewState.items.isNotEmpty()) {
                    OverviewList(
                        viewState = viewState,
                        listState = listState,
                        paddingValues = remainingPadding,
                        onRepeatMenuItemTapped = onRepeatMenuItemTapped,
                        onAnalyseMacrosMenuItemTapped = onAnalyseMacrosMenuItemTapped,
                        onDeleteMenuItemTapped = onDeleteMenuItemTapped,
                        onRecordImageTapped = onRecordImageTapped,
                        onRecordBodyTapped = onRecordBodyTapped,
                        onDailySummaryTapped = onDailySummaryTapped,
                        onWeeklySummaryTapped = onWeeklySummaryTapped,
                        onLoadMore = onLoadMore,
                    )
                } else if (viewState.showAddWidgetButton) {
                    WelcomeView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(remainingPadding),
                        onAddWidget = onAddWidget,
                        onRestoreFromCloud = onRestoreFromCloud,
                        restoreFromCloudInProgress = restoreFromCloudInProgress,
                        onRestoreFromLocalBackup = onRestoreFromLocalBackup,
                        restoreFromLocalBackupInProgress = restoreFromLocalBackupInProgress,
                    )
                }

                OverviewListTopActions(
                    visible = fabsVisible,
                    showSettingsButton = viewState.showSettingsButton,
                    topContentPadding = 0.dp,
                    onSettingsButtonTapped = onSettingsButtonTapped,
                )

                if (!viewState.showAddWidgetButton) {
                    val coroutineScope = rememberCoroutineScope()
                    // Fully qualified: an implicit ColumnScope receiver is in scope here (from
                    // the enclosing Column further up), which would otherwise shadow the
                    // top-level AnimatedVisibility with ColumnScope's extension overload.
                    androidx.compose.animation.AnimatedVisibility(
                        visible = fabsVisible,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            // Scaffold used to inset the FAB from the nav bar/gesture area (and,
                            // once expanded into a search field, lift it above the keyboard)
                            // itself via contentWindowInsets; now that this is a manual overlay,
                            // that has to be applied explicitly - navigationBars alone left the
                            // expanded search field stuck behind the IME.
                            .padding(WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues())
                            .padding(PaddingHalf),
                        enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
                    ) {
                        SearchFAB(
                            onSearch = {
                                onSearchTermChanged(it)
                            },
                            onSearchCleared = {
                                coroutineScope.launch {
                                    delay(200)
                                    listState.scrollToItem(0)
                                }
                            }
                        )
                    }
                }

                // Shown for as long as a Trends-triggered scroll is pending (see
                // OverviewViewModel.onScrollToDateRequested) - widening the paging window and
                // reloading from Room before the target day is available can take a moment,
                // which otherwise reads as the tap having done nothing.
                viewState.pendingScrollDateLabel?.let { dateLabel ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center),
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shadowElevation = 4.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = PaddingDefault, vertical = PaddingHalf),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PaddingHalf),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = stringResource(R.string.overview_content_scrolling_to_date, dateLabel),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OverviewListPreview() {
    PreviewContext {
        OverviewView(
            viewState = OverviewUiState(
                items = listOf(
                    ListUiModelDailySummary(
                        listItemId = 1L,
                        day = LocalDate.now(),
                        dayTitle = "Yesterday",
                        entries = listOf(
                            DailySummaryEntry(
                                title = "Calories",
                                progress0to1 = .15f,
                                progressLabel = "1005kcal",
                                targetRange0to1 = Range(.84f, .88f),
                                targetRangeLabel = "2.1-2.2k",
                                color = { it.caloriesColor },
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
                        imageFilename = "",
                        timestamp = "17:00",
                        nutrients = NutrientsUiModel(
                            calories = "8cal",
                            protein = "prot 8",
                            fat = "fat 4(2)",
                            carbs = "carb 9(9)",
                            salt = "sal 2",
                            fibre = "fib 4",
                        ),
                        showLoadingIndicator = false,
                    ),
                    ListUiModelRecord(
                        recordId = 3L,
                        title = "Title 2",
                        templateId = 4L,
                        imageFilename = "",
                        timestamp = "15:38",
                        nutrients = NutrientsUiModel(
                            calories = "8cal",
                            protein = "prot 8",
                            fat = "fat 4(2)",
                            carbs = "carb 9(9)",
                            salt = "sal 2",
                            fibre = "fib 4",
                        ),
                        showLoadingIndicator = false,
                    )
                ),
            ),
            onRepeatMenuItemTapped = {},
            onDeleteMenuItemTapped = {},
            onRecordImageTapped = {},
            onRecordBodyTapped = {},
            onUndoDeleteTapped = {},
            onUndoDeleteDismissed = {},
            onUndoDeleteSnackbarShown = {},
            onSearchTermChanged = {},
            onAnalyseMacrosMenuItemTapped = {},
            onSettingsButtonTapped = {},
            onDailySummaryTapped = {},
            onWeeklySummaryTapped = {},
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
            viewState = OverviewUiState(
                items = emptyList(),
                showAddWidgetButton = true,
            ),
            onRepeatMenuItemTapped = {},
            onDeleteMenuItemTapped = {},
            onRecordImageTapped = {},
            onRecordBodyTapped = {},
            onUndoDeleteTapped = {},
            onUndoDeleteDismissed = {},
            onUndoDeleteSnackbarShown = {},
            onSearchTermChanged = {},
            onAnalyseMacrosMenuItemTapped = {},
            onSettingsButtonTapped = {},
            onDailySummaryTapped = {},
            onWeeklySummaryTapped = {},
            onLoadMore = {},
        )
    }
}
