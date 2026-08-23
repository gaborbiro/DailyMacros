package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import dev.gaborbiro.dailymacros.design.PaddingHalf
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
    showSettingsButton: Boolean,
    topContentPadding: Dp,
    onSettingsButtonTapped: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topContentPadding),
    ) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.TopEnd),
            enter = slideInVertically(initialOffsetY = { -it * 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it * 2 }) + fadeOut(),
        ) {
            Row(
                modifier = Modifier.padding(PaddingHalf),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaddingHalf),
            ) {
                if (showSettingsButton) {
                    FloatingActionButton(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = onSettingsButtonTapped,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.overview_content_settings_cd),
                        )
                    }
                }
            }
        }
    }
}
