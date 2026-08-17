package dev.gaborbiro.dailymacros.features.overview.views

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
 * Always-visible actions pinned to the top of the Overview screen, independent of the list's
 * scroll position - a FAB (or a CTA pointing at it) that slid away on scroll could end up
 * stuck off-screen if the list's scroll-anchoring shifted its reported index (e.g. a new
 * record prepended while backgrounded), so nothing here reacts to scroll state.
 */
@Composable
internal fun OverviewListTopActions(
    showSettingsButton: Boolean,
    showSetTargetsCta: Boolean,
    topContentPadding: Dp,
    onSettingsButtonTapped: () -> Unit,
    onSetTargetsTapped: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topContentPadding),
    ) {
        Row(
            modifier = Modifier
                .padding(PaddingHalf)
                .align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaddingHalf),
        ) {
            if (showSettingsButton && showSetTargetsCta) {
                ListItemSetTargetsCta(onTapped = onSetTargetsTapped)
            }
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
