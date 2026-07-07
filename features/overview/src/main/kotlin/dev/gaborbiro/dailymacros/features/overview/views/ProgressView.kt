package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ProgressView(
    modifier: Modifier,
    progress0to1: Float,
    min0to1: Float,
    max0to1: Float,
    progressColor: Color,
    trackColor: Color,
    barHeight: Dp = 6.dp,
) {
    Canvas(modifier = modifier.height(barHeight)) {
        val radius = size.height / 2
        val cornerRadius = CornerRadius(radius, radius)

        // Track (full background)
        drawRoundRect(
            color = trackColor,
            cornerRadius = cornerRadius,
            size = size
        )

        // Progress fill
        val fraction = progress0to1 % 1f
        val progressWidth = fraction * size.width
        drawRoundRect(
            color = progressColor,
            topLeft = Offset(0f, 0f),
            size = Size(progressWidth, size.height),
            cornerRadius = cornerRadius
        )

        // Min target marker — green line, always on top, hidden only when min is unset (min0to1 == -1)
        if (min0to1 >= 0f) {
            val markerX = min0to1 * size.width
            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(markerX, 0f),
                end = Offset(markerX, size.height),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}