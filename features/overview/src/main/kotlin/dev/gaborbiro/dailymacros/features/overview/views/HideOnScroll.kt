package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Continuous scroll towards the end of the list needed before the FABs slide out. */
private val HideScrollThreshold = 40.dp

/** Continuous scroll back towards the start of the list needed before they slide back in. Larger
 *  than [HideScrollThreshold] on purpose: an accidental reappearance (e.g. from the few pixels of
 *  jitter as a finger lifts off) was the main annoyance. */
private val ShowScrollThreshold = 80.dp

/**
 * Hide-on-scroll visibility for floating controls over [listState], with hysteresis: only a
 * deliberate run of scrolling in one direction ([hideThreshold] forward / [showThreshold]
 * backward) flips it, and any scroll the other way restarts the count. Always visible when the
 * list is at its very start, where there'd be no room left to scroll back far enough.
 *
 * Deltas are measured by following the first visible item (by key) between layouts rather than
 * via ScrollableState.lastScrolledForward/Backward, which flip on a single pixel of movement -
 * that made the FABs pop back in at the end of most gestures. Following an item by key also
 * ignores programmatic jumps (scrollToItem) whose anchor item is no longer laid out, and stays
 * put when a new record is prepended, since Compose keeps the anchored item in place.
 */
@Composable
internal fun rememberHideOnScrollVisibility(
    listState: LazyListState,
    hideThreshold: Dp = HideScrollThreshold,
    showThreshold: Dp = ShowScrollThreshold,
): State<Boolean> {
    val visible = remember { mutableStateOf(true) }
    val density = LocalDensity.current
    val hideThresholdPx = with(density) { hideThreshold.toPx() }
    val showThresholdPx = with(density) { showThreshold.toPx() }

    LaunchedEffect(listState, hideThresholdPx, showThresholdPx) {
        var anchorKey: Any? = null
        var anchorOffset = 0
        // Positive: travelled forward (towards the end), negative: backward. Restarts from zero
        // on a change of direction.
        var travel = 0f

        snapshotFlow { listState.layoutInfo }.collect { layoutInfo ->
            val items = layoutInfo.visibleItemsInfo
            val anchor = anchorKey?.let { key -> items.firstOrNull { it.key == key } }
            if (anchor != null) {
                val delta = (anchorOffset - anchor.offset).toFloat()
                if (delta != 0f) {
                    travel = if (delta > 0f == travel > 0f) travel + delta else delta
                    if (travel >= hideThresholdPx) {
                        visible.value = false
                    } else if (-travel >= showThresholdPx) {
                        visible.value = true
                    }
                }
            } else {
                travel = 0f
            }
            if (!listState.canScrollBackward) {
                visible.value = true
                travel = 0f
            }
            val first = items.firstOrNull()
            anchorKey = first?.key
            anchorOffset = first?.offset ?: 0
        }
    }
    return visible
}
