package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.overview.R

@Composable
internal fun OverviewListTopActions(
    showSettingsButton: Boolean,
    listAtTop: Boolean,
    topContentPadding: Dp,
    onSettingsButtonTapped: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topContentPadding),
    ) {
        val buttonsOffset by animateDpAsState(
            targetValue = if (listAtTop) 0.dp else 88.dp,
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
