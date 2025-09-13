package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import android.util.Range
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.design.ExtraColors
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
import kotlin.math.hypot
import kotlin.math.max

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
    onCoachMarkDismissed: () -> Unit,
) {
    val listState = rememberLazyListState()

    var targetBounds by remember { mutableStateOf<Rect?>(null) }
    var showCoachMark by remember(viewState.showCoachMark) { mutableStateOf(viewState.showCoachMark) }

    Box(Modifier.fillMaxSize()) {
        val repeatIcon = painterResource(R.drawable.ic_exposure_plus_1)
        val macrosIcon = painterResource(R.drawable.ic_nutrition)
        val detailsIcon = painterResource(R.drawable.ic_topic)
        val deleteIcon = painterResource(R.drawable.ic_delete)
        val menuIcons = remember {
            MenuIcons(
                repeat = repeatIcon,
                macros = macrosIcon,
                details = detailsIcon,
                delete = deleteIcon
            )
        }
        var expandedId by remember { mutableStateOf<Any?>(null) }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(PaddingDefault),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 24.dp,
                bottom = paddingValues.calculateBottomPadding() + 86.dp
            ),
            state = listState,
        ) {
            itemsIndexed(
                items = viewState.items,
                key = { _, item -> item.listItemId },
                contentType = { _, item -> item.contentType },
            ) { _, item ->
                when (item) {
                    is ListUIModelRecord -> {
                        val onOpen = remember(item.listItemId) { { expandedId = item.listItemId } }
                        val onRepeat =
                            remember(item.listItemId) { { onRepeatMenuItemTapped(item.listItemId) } }
                        val onMacros =
                            remember(item.listItemId) { { onMacrosMenuItemTapped(item.listItemId) } }
                        val onDetails =
                            remember(item.listItemId) { { onDetailsMenuItemTapped(item.listItemId) } }
                        val onDelete =
                            remember(item.listItemId) { { onDeleteRecordMenuItemTapped(item.listItemId) } }
                        val onDismiss = remember { { expandedId = null } }

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
                                    if (expandedId != null) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            IconButton(
                modifier = Modifier
                    .padding(PaddingHalf)
                    .align(Alignment.TopEnd)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        val size = coords.size
                        targetBounds = Rect(
                            offset = pos,
                            size = Size(
                                size.width.toFloat(),
                                size.height.toFloat()
                            )
                        )
                    },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
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

        if (showCoachMark) {
            CoachMarkOverlay(
                targetRect = targetBounds,
                text = "Set some goals here",
                onDismiss = {
                    showCoachMark = false
                    onCoachMarkDismissed()
                }
            )
            BackHandler {
                showCoachMark = false
                onCoachMarkDismissed()
            }
        }
    }

    // Prefetch and scroll helpers
    PrefetchRecordThumbnails(
        listState = listState,
        items = viewState.items,
        ahead = 12,
        behind = 4
    )
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

@Composable
internal fun CoachMarkOverlay(
    targetRect: Rect?,
    text: String,
    scrimColor: Color = Color.Black.copy(alpha = 0.6f),
    bubbleMaxWidth: Dp = 220.dp,
    spotlightPadding: Dp = 12.dp,
    onDismiss: () -> Unit,
) {
    if (targetRect == null) return

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val paddingPx = with(density) { spotlightPadding.toPx() }

    val buttonCenter = targetRect.center
    val maxRadius = listOf(
        hypot(buttonCenter.x, buttonCenter.y),
        hypot(screenWidthPx - buttonCenter.x, buttonCenter.y),
        hypot(buttonCenter.x, screenHeightPx - buttonCenter.y),
        hypot(screenWidthPx - buttonCenter.x, screenHeightPx - buttonCenter.y)
    ).max()
    val targetRadius = max(targetRect.width, targetRect.height) / 2f + paddingPx

    val radius = remember { Animatable(maxRadius) }
    var animationFinished by remember { mutableStateOf(false) }

    LaunchedEffect(targetRect) {
        radius.animateTo(
            targetValue = targetRadius,
            animationSpec = tween(800, easing = LinearOutSlowInEasing)
        )
        animationFinished = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInteropFilter { motionEvent ->
                val dx = motionEvent.x - buttonCenter.x
                val dy = motionEvent.y - buttonCenter.y
                val distance = hypot(dx, dy)

                if (distance <= radius.value) {
                    // Inside spotlight
                    if (motionEvent.action == MotionEvent.ACTION_UP) {
                        onDismiss() // dismiss after button got the click
                    }
                    false // let the event fall through to the button
                } else {
                    // Outside spotlight
                    if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                        onDismiss()
                    }
                    true // consume so outside taps don't leak through
                }
            }
    ) {
        // Scrim drawing
        Canvas(Modifier.matchParentSize()) {
            val path = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                addOval(
                    Rect(
                        left = buttonCenter.x - radius.value,
                        top = buttonCenter.y - radius.value,
                        right = buttonCenter.x + radius.value,
                        bottom = buttonCenter.y + radius.value
                    )
                )
                fillType = PathFillType.EvenOdd
            }
            drawPath(path, scrimColor)
        }

        if (animationFinished) {
            val bubbleY = with(density) { targetRect.bottom.toDp() + 12.dp }
            val bubbleX = with(density) {
                (targetRect.right.toDp() - bubbleMaxWidth).coerceAtLeast(8.dp)
            }

            Column(
                Modifier
                    .offset(x = bubbleX, y = bubbleY)
                    .widthIn(max = bubbleMaxWidth)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                Text(text, color = Color.Black, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}


@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OverviewListPreview() {
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
                onCoachMarkDismissed = {}
            )
        }
    }
}
