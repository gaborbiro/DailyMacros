package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.features.overview.R

/**
 * Actions pinned to the top of the Overview screen. [visible] is driven by
 * listState.lastScrolledForward/Backward (see OverviewView.kt), not listState.firstVisibleItemIndex -
 * an earlier index-based version of this could end up stuck off-screen if the list's
 * scroll-anchoring shifted its reported "top" index (e.g. a new record prepended while
 * backgrounded), so it was removed.
 */
@Composable
internal fun OverviewListTopActions(
    visible: Boolean,
    showTopActions: Boolean,
    topContentPadding: Dp,
    onTrendsButtonTapped: () -> Unit,
    onSettingsButtonTapped: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topContentPadding),
    ) {
        AnimatedVisibility(
            visible = visible && showTopActions,
            modifier = Modifier.align(Alignment.TopEnd),
            enter = slideInVertically(initialOffsetY = { -it * 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it * 2 }) + fadeOut(),
        ) {
            // One pill holding Trends + an overflow menu (à la Samsung Health), rather than a
            // FAB per destination: Trends is the frequent action and gets its own button, while
            // rarely-visited destinations (just Settings for now) live behind the overflow.
            Surface(
                modifier = Modifier.padding(PaddingHalf),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shadowElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier.padding(PaddingQuarter),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onTrendsButtonTapped) {
                        Icon(
                            imageVector = Icons.Outlined.BarChart,
                            contentDescription = stringResource(R.string.overview_content_trends_cd),
                        )
                    }
                    Box {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.overview_content_more_options_cd),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.overview_content_menu_settings)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSettingsButtonTapped()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
