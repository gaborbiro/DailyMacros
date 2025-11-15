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

        // Target range start highlight (only on first coat)
        if (progress0to1 < min0to1) {
            val start = min0to1 * size.width
            drawArc(
                color = progressColor,
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(start - radius, (size.height / 2) - radius),
                size = Size(radius * 2, radius * 2)
            )
        }

        // Target range end highlight (only on first coat)
//        if (progress0to1 < 1f) {
//            val end = max0to1 * size.width
//            drawArc(
//                color = progressColor,
//                startAngle = 270f,
//                sweepAngle = 180f,
//                useCenter = true,
//                topLeft = Offset(end - radius, (size.height / 2) - radius),
//                size = Size(radius * 2, radius * 2)
//            )
//        }

        // Progress fill
        val fraction = progress0to1 % 1f
        val progressWidth = fraction * size.width
        drawRoundRect(
            color = progressColor,
            topLeft = Offset(0f, 0f),
            size = Size(progressWidth, size.height),
            cornerRadius = cornerRadius
        )
    }
}