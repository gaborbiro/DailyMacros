package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import android.util.Range
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.ExtraColors
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingDouble
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelMacroProgress
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelRecord
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem
import dev.gaborbiro.dailymacros.features.common.model.MacrosUIModel
import dev.gaborbiro.dailymacros.features.common.view.LocalImageStore
import dev.gaborbiro.dailymacros.features.common.view.PreviewImageStoreProvider
import dev.gaborbiro.dailymacros.features.overview.model.OverviewViewState
import kotlinx.coroutines.launch

@Composable
internal fun OverviewList(
    viewState: OverviewViewState,
    paddingValues: PaddingValues,
    onRepeatMenuItemTapped: (id: Long) -> Unit,
    onDetailsMenuItemTapped: (id: Long) -> Unit,
    onDeleteRecordMenuItemTapped: (id: Long) -> Unit,
    onMacrosMenuItemTapped: (id: Long) -> Unit,
    onRecordImageTapped: (id: Long) -> Unit,
    onRecordBodyTapped: (id: Long) -> Unit,
    onSettingsButtonTapped: () -> Unit,
) {
    val listState: LazyListState = rememberLazyListState()

    // --- visibility + animation state for header ---
    var toolbarHeightPx by remember { mutableIntStateOf(0) }
    var showToolbar by remember { mutableStateOf(true) }
    val headerOffset by animateIntAsState(
        targetValue = if (showToolbar) 0 else -toolbarHeightPx,
        label = "headerOffset"
    )

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 20
        }.collect { atTop ->
            showToolbar = atTop
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = paddingValues.calculateTopPadding())
            .onGloballyPositioned {
                toolbarHeightPx =
                    it.size.height + it.positionOnScreen().y.toInt()
            }
            .offset { IntOffset(x = 0, y = headerOffset) }
            .zIndex(1f), // ensure it’s above WebView
    ) {
        Spacer(
            modifier = Modifier
                .weight(1f)
        )
        FilledTonalIconButton(
            modifier = Modifier
                .padding(PaddingHalf),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            onClick = onSettingsButtonTapped,
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings Button",
            )
        }
    }

    PrefetchRecordThumbnails(
        listState = listState,
        items = viewState.items,
        ahead = 12,
        behind = 4
    )

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
            key = { index, item -> item.listItemId },
            contentType = { index, item -> item.contentType },
        ) { index, item ->
            val onOpen = remember(item.listItemId) { { expandedId = item.listItemId } }
            val onRepeat = remember(item.listItemId) { { onRepeatMenuItemTapped(item.listItemId) } }
            val onMacros = remember(item.listItemId) { { onMacrosMenuItemTapped(item.listItemId) } }
            val onDetails =
                remember(item.listItemId) { { onDetailsMenuItemTapped(item.listItemId) } }
            val onDelete =
                remember(item.listItemId) { { onDeleteRecordMenuItemTapped(item.listItemId) } }
            val onDismiss = remember { { expandedId = null } }

            when (item) {
                is ListUIModelRecord -> {
                    ListItemRecord(
                        record = item,
                        onRecordImageTapped = onRecordImageTapped,
                        onRecordBodyTapped = onRecordBodyTapped,
                    ) {
                        RowMenu(
                            expanded = expandedId == item.listItemId,
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

                is ListUIModelMacroProgress -> {
                    ListItemMacroProgressBars(
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
    ScrollToTopView(listState)
}

@Composable
private fun PrefetchRecordThumbnails(
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
                    val name = (items[i] as? ListUIModelRecord)?.images?.firstOrNull() ?: continue
                    if (seen.addIfNew(name)) launch { store.read(name, thumbnail = true) }
                }

                // behind (small)
                val behindStart = (first - behind).coerceAtLeast(0)
                val behindEnd = (first - 1).coerceAtLeast(-1)
                for (i in behindStart..behindEnd) {
                    val name = (items[i] as? ListUIModelRecord)?.images?.firstOrNull() ?: continue
                    if (seen.addIfNew(name)) launch { store.read(name, thumbnail = true) }
                }
            }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NotesListPreview() {
    AppTheme {
        PreviewImageStoreProvider {
            OverviewList(
                paddingValues = PaddingValues(),
                viewState = OverviewViewState(
                    items = listOf(
                        ListUIModelMacroProgress(
                            listItemId = 1L,
                            dayTitle = "Yesterday",
                            progress = listOf(
                                MacroProgressItem(
                                    title = "Calories",
                                    progress0to1 = .15f,
                                    progressLabel = "1005kcal",
                                    targetRange0to1 = Range(.84f, .88f),
                                    rangeLabel = "2.1-2.2k",
                                    color = ExtraColors.calorieColor,
                                ),
                                MacroProgressItem(
                                    title = "Protein",
                                    progress0to1 = .0809f,
                                    progressLabel = "110g",
                                    targetRange0to1 = Range(.8095f, .9047f),
                                    rangeLabel = "170-190g",
                                    color = ExtraColors.proteinColor,
                                ),
                                MacroProgressItem(
                                    title = "Fat",
                                    progress0to1 = .2121f,
                                    progressLabel = "30g",
                                    targetRange0to1 = Range(.6818f, .9091f),
                                    rangeLabel = "45-60g",
                                    color = ExtraColors.fatColor,
                                ),
                                MacroProgressItem(
                                    title = "Carbs",
                                    progress0to1 = .1818f,
                                    progressLabel = "105g",
                                    targetRange0to1 = Range(.6818f, .9091f),
                                    rangeLabel = "150-200g",
                                    color = ExtraColors.carbsColor,
                                ),
                                MacroProgressItem(
                                    title = "Sugar",
                                    progress0to1 = .2955f,
                                    progressLabel = "35g",
                                    targetRange0to1 = Range(.9091f, .9091f),
                                    rangeLabel = "<40g/<25g added",
                                    color = ExtraColors.carbsColor,
                                ),
                                MacroProgressItem(
                                    title = "Salt",
                                    progress0to1 = .0f,
                                    progressLabel = "0g",
                                    targetRange0to1 = Range(.9091f, .9091f),
                                    rangeLabel = "<5g (≈2g Na)",
                                    color = ExtraColors.saltColor,
                                ),
                                MacroProgressItem(
                                    title = "Fibre",
                                    progress0to1 = .0f,
                                    progressLabel = "0g",
                                    targetRange0to1 = Range(.9091f, .9091f),
                                    rangeLabel = "30-38g",
                                    color = ExtraColors.fibreColor,
                                ),
                            )
                        ),
                        ListUIModelRecord(
                            recordId = 2L,
                            title = "Title",
                            templateId = 2L,
                            images = listOf("", ""),
                            timestamp = "17:00",
                            macros = MacrosUIModel(
                                calories = "8cal",
                                protein = "prot 8",
                                fat = "fat 4(2)",
                                carbs = "carb 9(9)",
                                salt = "sal 2",
                                fibre = "fib 4",
                            ),
                        ),
                        ListUIModelRecord(
                            recordId = 3L,
                            title = "Title 2",
                            templateId = 4L,
                            images = listOf("", ""),
                            timestamp = "15:38",
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
                onMacrosMenuItemTapped = {},
                onSettingsButtonTapped = {},
            )
        }
    }
}
