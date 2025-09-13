package dev.gaborbiro.dailymacros.features.common.view

import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.hypot
import kotlin.math.max

internal fun Modifier.coachMarkOverlayAnchor(targetBounds: (Rect) -> Unit): Modifier {
    return onGloballyPositioned { coords ->
        val pos = coords.positionInRoot()
        val size = coords.size
        targetBounds(
            Rect(
                offset = pos,
                size = Size(
                    size.width.toFloat(),
                    size.height.toFloat()
                )
            )
        )
    }
}

@Composable
internal fun CoachMarkOverlay(
    targetRect: Rect?,
    text: String,
    scrimColor: Color = Color.Companion.Black.copy(alpha = 0.6f),
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
        Modifier.Companion
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
            .zIndex(10f)
    ) {
        // Scrim drawing
        Canvas(Modifier.Companion.matchParentSize()) {
            val path = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                addRect(
                    Rect(
                        left = buttonCenter.x - radius.value,
                        top = buttonCenter.y - radius.value,
                        right = buttonCenter.x + radius.value,
                        bottom = buttonCenter.y + radius.value
                    )
                )
                fillType = PathFillType.Companion.EvenOdd
            }
            drawPath(path, scrimColor)
        }

        if (animationFinished) {
            val bubbleY = with(density) { targetRect.bottom.toDp() + 12.dp }
            val bubbleX = with(density) {
                (targetRect.right.toDp() - bubbleMaxWidth).coerceAtLeast(8.dp)
            }

            Column(
                Modifier.Companion
                    .offset(x = bubbleX, y = bubbleY)
                    .widthIn(max = bubbleMaxWidth)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Companion.White)
                    .padding(12.dp)
            ) {
                Text(text, color = Color.Companion.Black, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
