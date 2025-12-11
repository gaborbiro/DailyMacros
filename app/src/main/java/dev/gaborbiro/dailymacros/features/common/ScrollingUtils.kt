package dev.gaborbiro.dailymacros.features.common

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
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
import dev.gaborbiro.dailymacros.util.px
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.time.debounce
import java.time.Duration


@Composable
@OptIn(FlowPreview::class)
fun Modifier.verticalScrollWithBar(
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

    // Detect scroll changes and manage fade
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .debounce(Duration.ofSeconds(1))
            .collect {
                // Only runs after 1000ms of no scrolling
                if (autoFade) {
                    animate(
                        initialValue = alpha, // Start from current alpha
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 500)
                    ) { value, _ ->
                        alpha = value
                    }
                }
            }
    }

    // Immediate fade-in on scroll
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .collect { alpha = 1f } // Immediate fade-in
    }

    val context = LocalContext.current

    this
        .drawWithContent {
            // Draw the column's content
            this.drawContent()

            val viewportHeight =
                this.size.height - topPadding.px(context) - bottomPadding.px(context)
            val totalContentHeight = scrollState.maxValue.toFloat() + viewportHeight

            if (viewportHeight != totalContentHeight) {
                val scrollValue = scrollState.value.toFloat()
                // Compute scrollbar height and position
                val scrollBarHeight = (viewportHeight / totalContentHeight) * viewportHeight

                val scrollBarStartOffset =
                    (scrollValue / totalContentHeight) * viewportHeight + topPadding.px(context)
                val rightPadding = rightPadding.px(context)

                // Draw the track (optional)
                if (showScrollBarTrack) {
                    drawRoundRect(
                        cornerRadius = CornerRadius(scrollBarCornerRadius),
                        color = scrollBarTrackColor,
                        topLeft = Offset(
                            x = this.size.width - rightPadding - width.px(context),
                            y = topPadding.px(context),
                        ),
                        size = Size(
                            width = width.px(context),
                            height = viewportHeight,
                        ),
                    )
                }

                // Draw the scrollbar
                drawRoundRect(
                    cornerRadius = CornerRadius(scrollBarCornerRadius),
                    color = scrollBarColor.copy(alpha = scrollBarColor.alpha * alpha),
                    topLeft = Offset(
                        x = this.size.width - rightPadding - width.px(context),
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

@Composable
@OptIn(FlowPreview::class)
fun Modifier.horizontalScrollWithBar(
    scrollState: ScrollState = rememberScrollState(),
    height: Dp = 4.dp,
    showScrollBarTrack: Boolean = false,
    scrollBarTrackColor: Color = Color.LightGray.copy(alpha = .5f),
    scrollBarColor: Color = LocalExtraColorScheme.current.scrollbarColor,
    scrollBarCornerRadius: Float = 8f,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    autoFade: Boolean = true,
): Modifier = composed {
    var alpha: Float by remember { mutableFloatStateOf(0f) }

    // Detect scroll changes and manage fade
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

    // Immediate fade-in on scroll
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .collect { alpha = 1f }
    }

    val context = LocalContext.current

    this
        .drawWithContent {
            // Draw the row's content
            drawContent()

            val viewportWidth = size.width - startPadding.px(context) - endPadding.px(context)
            val totalContentWidth = scrollState.maxValue.toFloat() + viewportWidth

            if (viewportWidth != totalContentWidth) {
                val scrollPx = scrollState.value.toFloat()
                val scrollBarWidth = (viewportWidth / totalContentWidth) * viewportWidth
                val scrollBarX =
                    (scrollPx / totalContentWidth) * viewportWidth + startPadding.px(context)

                val bottomOffset = bottomPadding.px(context)

                // Draw the track (optional)
                if (showScrollBarTrack) {
                    drawRoundRect(
                        color = scrollBarTrackColor,
                        cornerRadius = CornerRadius(scrollBarCornerRadius),
                        topLeft = Offset(
                            x = startPadding.px(context),
                            y = size.height - bottomOffset - height.px(context),
                        ),
                        size = Size(
                            width = viewportWidth,
                            height = height.px(context),
                        )
                    )
                }

                // Draw the scrollbar
                drawRoundRect(
                    color = scrollBarColor.copy(alpha = scrollBarColor.alpha * alpha),
                    cornerRadius = CornerRadius(scrollBarCornerRadius),
                    topLeft = Offset(
                        x = scrollBarX,
                        y = size.height - bottomOffset - height.px(context)
                    ),
                    size = Size(
                        width = scrollBarWidth,
                        height = height.px(context),
                    )
                )
            }
        }
        .horizontalScroll(scrollState)
}
