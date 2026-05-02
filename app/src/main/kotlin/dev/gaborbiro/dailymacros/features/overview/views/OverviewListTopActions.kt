package dev.gaborbiro.dailymacros.features.overview.views

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingHalf

@Composable
internal fun OverviewListTopActions(
    showSettingsButton: Boolean,
    showTrendsButton: Boolean,
    showCoachMark: Boolean,
    listAtTop: Boolean,
    topContentPadding: Dp,
    onSettingsButtonTapped: () -> Unit,
    onTrendsButtonTapped: () -> Unit,
    onCoachMarkDismissed: () -> Unit,
) {
    var targetBounds by remember { mutableStateOf<Rect?>(null) }
    var coachMarkVisible by remember(showCoachMark) { mutableStateOf(showCoachMark) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topContentPadding),
    ) {
        val buttonsOffset by animateDpAsState(
            targetValue = if (listAtTop) 0.dp else 72.dp,
            animationSpec = tween(
                durationMillis = 220,
                easing = FastOutSlowInEasing,
            ),
            label = "ButtonsSlide",
        )

        Column(
            modifier = Modifier
                .padding(PaddingHalf)
                .align(Alignment.TopEnd)
                .offset(x = buttonsOffset),
        ) {
            if (showSettingsButton) {
                IconButton(
                    modifier = Modifier
                        .coachMarkOverlayAnchor {
                            targetBounds = it
                        },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    onClick = onSettingsButtonTapped,
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings Button",
                    )
                }
            }
            if (showTrendsButton) {
                Spacer(modifier = Modifier.padding(PaddingHalf))
                IconButton(
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    onClick = onTrendsButtonTapped,
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Trends Button",
                    )
                }
            }
        }
    }

    if (coachMarkVisible) {
        CoachMarkOverlay(
            targetRect = targetBounds,
            text = "Set some goals here",
            onDismiss = {
                coachMarkVisible = false
                onCoachMarkDismissed()
            },
        )
        BackHandler {
            coachMarkVisible = false
            onCoachMarkDismissed()
        }
    }
}
