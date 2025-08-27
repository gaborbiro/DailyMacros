package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressUIModel
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.common.view.LocalImageStore
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
) {
    val listState: LazyListState = rememberLazyListState()

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
