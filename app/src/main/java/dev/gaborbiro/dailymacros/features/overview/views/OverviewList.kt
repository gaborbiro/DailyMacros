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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.DailyMacrosColors
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressUIModel
import dev.gaborbiro.dailymacros.features.common.model.MacrosUIModel
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.common.view.LocalImageStore
import dev.gaborbiro.dailymacros.features.common.view.PreviewImageStoreProvider
import dev.gaborbiro.dailymacros.features.overview.model.OverviewViewState
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
internal fun OverviewList(
    viewState: OverviewViewState,
    onRepeatMenuItemTapped: (id: Long) -> Unit,
    onDetailsMenuItemTapped: (id: Long) -> Unit,
    onDeleteRecordMenuItemTapped: (id: Long) -> Unit,
    onMacrosMenuItemTapped: (id: Long) -> Unit,
    onRecordImageTapped: (id: Long) -> Unit,
    onRecordBodyTapped: (id: Long) -> Unit,
    onUndoDeleteTapped: () -> Unit,
    onUndoDeleteDismissed: () -> Unit,
    onUndoDeleteSnackbarShown: () -> Unit,
    onSearchTermChanged: (String?) -> Unit,
) {
    val listState: LazyListState = rememberLazyListState()
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

    PrefetchRecordThumbnails(
        listState = listState,
        items = viewState.items,
        ahead = 12,
        behind = 4
    )

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
        val repeatIcon = painterResource(R.drawable.ic_exposure_plus_1)
        val macrosIcon = painterResource(R.drawable.ic_nutrition)
        val detailsIcon = painterResource(R.drawable.ic_topic)
        val deleteIcon = painterResource(R.drawable.ic_delete)
        val menuIcons = remember {
            MenuIcons(repeat = repeatIcon, macros = macrosIcon, details = detailsIcon, delete = deleteIcon)
        }
        var expandedId by remember { mutableStateOf<Any?>(null) }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(PaddingDefault),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 86.dp
            ),
            state = listState,
        ) {
            itemsIndexed(
                items = viewState.items,
                key = { index, item -> item.id },
                contentType = { index, item -> item.contentType },
            ) { index, item ->
                val onOpen = remember(item.id) { { expandedId = item.id } }
                val onRepeat = remember(item.id) { { onRepeatMenuItemTapped(item.id) } }
                val onMacros = remember(item.id) { { onMacrosMenuItemTapped(item.id) } }
                val onDetails = remember(item.id) { { onDetailsMenuItemTapped(item.id) } }
                val onDelete = remember(item.id) { { onDeleteRecordMenuItemTapped(item.id) } }
                val onDismiss = remember { { expandedId = null } }

                when (item) {
                    is RecordUIModel -> {
                        ListItemRecord(
                            record = item,
                            onRecordImageTapped = onRecordImageTapped,
                            onRecordBodyTapped = onRecordBodyTapped,
                        ) {
                            RowMenu(
                                expanded = expandedId == item.id,
                                onOpen = onOpen,
                                onDismiss = onDismiss,
                                icons = menuIcons,
                                onRepeat = onRepeat,
                                onMacros = onMacros,
                                onDetails = onDetails,
                                onDelete = onDelete,
                            )
                        }
                    }

                    is MacroProgressUIModel -> {
                        ListItemMacroProgressBars(
                            modifier = Modifier
                                .let {
                                    if (index > 0) {
                                        it.padding(top = PaddingDefault)
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
        ScrollToTopView(listState)
    }
}

@Composable
fun PrefetchRecordThumbnails(
    listState: LazyListState,
    items: List<Any>,
    ahead: Int = 12,
    behind: Int = 4,
) {
    val store = LocalImageStore.current

    // tiny LRU to avoid spamming the same names; keep last ~256 requests
    val seen = remember {
        object {
            private val q = ArrayDeque<String>()
            private val s = HashSet<String>()
            fun addIfNew(k: String): Boolean {
                if (!s.add(k)) return false
                q.addLast(k)
                if (q.size > 256) s.remove(q.removeFirst())
                return true
            }
        }
    }

    LaunchedEffect(items, listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visible ->
                if (visible.isEmpty()) return@collect
                val first = visible.first().index
                val last = visible.last().index

                // ahead
                val aheadStart = (last + 1).coerceAtLeast(0)
                val aheadEnd = (last + ahead).coerceAtMost(items.lastIndex)
                for (i in aheadStart..aheadEnd) {
                    val name = (items[i] as? RecordUIModel)?.images?.firstOrNull() ?: continue
                    if (seen.addIfNew(name)) launch { store.read(name, thumbnail = true) }
                }

                // behind (small)
                val behindStart = (first - behind).coerceAtLeast(0)
                val behindEnd = (first - 1).coerceAtLeast(-1)
                for (i in behindStart..behindEnd) {
                    val name = (items[i] as? RecordUIModel)?.images?.firstOrNull() ?: continue
                    if (seen.addIfNew(name)) launch { store.read(name, thumbnail = true) }
                }
            }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NotesListPreview() {
    DailyMacrosTheme {
        PreviewImageStoreProvider {
            OverviewList(
                viewState = OverviewViewState(
                    items = listOf(
                        MacroProgressUIModel(
                            date = LocalDate.now(),
                            macros = listOf(
                                MacroProgressItem(
                                    title = "Calories",
                                    progress = .15f,
                                    progressLabel = "1005 cal",
                                    range = Range(.84f, .88f),
                                    rangeLabel = "2.1-2.2kcal",
                                    color = DailyMacrosColors.calorieColor,
                                ),
                                MacroProgressItem(
                                    title = "Protein",
                                    progress = .0809f,
                                    progressLabel = "110g",
                                    range = Range(.8095f, .9047f),
                                    rangeLabel = "170-190g",
                                    color = DailyMacrosColors.proteinColor,
                                ),
                                MacroProgressItem(
                                    title = "Fat",
                                    progress = .2121f,
                                    progressLabel = "30g",
                                    range = Range(.6818f, .9091f),
                                    rangeLabel = "45-60g",
                                    color = DailyMacrosColors.fatColor,
                                ),
                                MacroProgressItem(
                                    title = "Carbs",
                                    progress = .1818f,
                                    progressLabel = "105g",
                                    range = Range(.6818f, .9091f),
                                    rangeLabel = "150-200g",
                                    color = DailyMacrosColors.carbsColor,
                                ),
                                MacroProgressItem(
                                    title = "Sugar",
                                    progress = .2955f,
                                    progressLabel = "35g",
                                    range = Range(.9091f, .9091f),
                                    rangeLabel = "<40g ttl., <25g",
                                    color = DailyMacrosColors.carbsColor,
                                ),
                                MacroProgressItem(
                                    title = "Salt",
                                    progress = .0f,
                                    progressLabel = "0g",
                                    range = Range(.9091f, .9091f),
                                    rangeLabel = "<5g (≈2g Na)",
                                    color = DailyMacrosColors.saltColor,
                                ),
                                MacroProgressItem(
                                    title = "Fibre",
                                    progress = .0f,
                                    progressLabel = "0g",
                                    range = Range(.9091f, .9091f),
                                    rangeLabel = "30-38g",
                                    color = DailyMacrosColors.fibreColor,
                                ),
                            )
                        ),
                        RecordUIModel(
                            recordId = 1L,
                            title = "Title",
                            templateId = 1L,
                            images = listOf("", ""),
                            timestamp = "2022-01-01 00:00:00",
                            macros = MacrosUIModel(
                                calories = "8cal",
                                protein = "prot 8",
                                fat = "fat 4(2)",
                                carbs = "carb 9(9)",
                                salt = "sal 2",
                                fibre = "fib 4",
                            ),
                        ),
                        RecordUIModel(
                            recordId = 2L,
                            title = "Title 2",
                            templateId = 1L,
                            images = listOf("", ""),
                            timestamp = "2022-05-01 00:00:00",
                            macros = MacrosUIModel(
                                calories = "8cal",
                                protein = "prot 8",
                                fat = "fat 4(2)",
                                carbs = "carb 9(9)",
                                salt = "sal 2",
                                fibre = "fib 4",
                            ),
                        )
                    ),
                ),
                onRepeatMenuItemTapped = {},
                onDetailsMenuItemTapped = {},
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
}
