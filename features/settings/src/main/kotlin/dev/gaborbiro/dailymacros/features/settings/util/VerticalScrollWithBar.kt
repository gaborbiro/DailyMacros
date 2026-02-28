package dev.gaborbiro.dailymacros.features.settings.util

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.LocalExtraColorScheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.time.debounce
import java.time.Duration

@Composable
@OptIn(FlowPreview::class)
internal fun Modifier.verticalScrollWithBar(
    scrollState: ScrollState = rememberScrollState(),
    width: Dp = 4.dp,
    showScrollBarTrack: Boolean = false,
    scrollBarTrackColor: Color = Color.LightGray.copy(alpha = .5f),
    scrollBarColor: Color = LocalExtraColorScheme.current.scrollbarColor,
    scrollBarCornerRadius: Float = 8f,
    rightPadding: Dp = 0.dp,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    autoFade: Boolean = true,
): Modifier = composed {
    var alpha: Float by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .debounce(Duration.ofSeconds(1))
            .collect {
                if (autoFade) {
                    animate(
                        initialValue = alpha,
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 500)
                    ) { value, _ ->
                        alpha = value
                    }
                }
            }
    }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .collect { alpha = 1f }
    }

    val context = LocalContext.current

    this
        .drawWithContent {
            this.drawContent()

            val viewportHeight =
                this.size.height - topPadding.px(context) - bottomPadding.px(context)
            val totalContentHeight = scrollState.maxValue.toFloat() + viewportHeight

            if (viewportHeight != totalContentHeight) {
                val scrollValue = scrollState.value.toFloat()
                val scrollBarHeight = (viewportHeight / totalContentHeight) * viewportHeight

                val scrollBarStartOffset =
                    (scrollValue / totalContentHeight) * viewportHeight + topPadding.px(context)
                val rightPaddingPx = rightPadding.px(context)

                if (showScrollBarTrack) {
                    drawRoundRect(
                        cornerRadius = CornerRadius(scrollBarCornerRadius),
                        color = scrollBarTrackColor,
                        topLeft = Offset(
                            x = this.size.width - rightPaddingPx - width.px(context),
                            y = topPadding.px(context),
                        ),
                        size = Size(
                            width = width.px(context),
                            height = viewportHeight,
                        ),
                    )
                }

                drawRoundRect(
                    cornerRadius = CornerRadius(scrollBarCornerRadius),
                    color = scrollBarColor.copy(alpha = scrollBarColor.alpha * alpha),
                    topLeft = Offset(
                        x = this.size.width - rightPaddingPx - width.px(context),
                        y = scrollBarStartOffset,
                    ),
                    size = Size(
                        width = width.px(context),
                        height = scrollBarHeight,
                    )
                )
            }
        }
        .verticalScroll(scrollState)
}
