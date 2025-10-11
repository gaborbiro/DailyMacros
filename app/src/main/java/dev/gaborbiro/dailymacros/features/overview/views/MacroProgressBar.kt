package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import android.util.Range
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.design.ExtraColors
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem
import kotlinx.coroutines.delay

@Composable
internal fun MacroProgressBar(
    modifier: Modifier,
    model: MacroProgressItem,
    rowIndex: Int,
    totalRowCount: Int,
) {
    val animated = remember { Animatable(0f) }

    val initialPause = 500 // ms before first row
    val stagger = 300 // ms per row
    val baseDuration = 400 // ms for the last row's animation

    // Total time for the whole wave (pause + stagger + last row anim)
    val totalAnimDuration =
        remember(totalRowCount) { initialPause + baseDuration + (totalRowCount - 1) * stagger }

    LaunchedEffect(model.progress0to1, totalRowCount) {
        // Delay for this row = pause + rowIndex * stagger
        delay(initialPause + rowIndex * stagger.toLong())

        // Remaining time until the global end
        val timeLeft = totalAnimDuration - (initialPause + rowIndex * stagger)
        if (timeLeft > 0) {
            animated.animateTo(
                targetValue = model.progress0to1,
                animationSpec = tween(
                    durationMillis = timeLeft,
                    easing = OvershootInterpolatorEasing
                )
            )
        } else {
            animated.snapTo(model.progress0to1)
        }
    }

    val progress = animated.value
    val onBackground = MaterialTheme.colorScheme.onBackground

    val (barColor, trackColor) = remember(progress) {
        layeredColors(progress0to1 = progress, base = model.color, onBackground = onBackground)
    }

    Column(
        modifier = modifier
            .padding(horizontal = PaddingHalf)
            .padding(top = 7.dp),
    ) {
        MacroProgressBar(
            modifier = Modifier.fillMaxWidth(),
            progress0to1 = progress,
            min0to1 = model.targetRange0to1.lower,
            max0to1 = model.targetRange0to1.upper,
            progressColor = barColor,
            trackColor = trackColor,
        )

        Row(
            modifier = Modifier
                .padding(top = 2.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = model.progressLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (model.progress0to1 > 1f) Color.Red else Color.Unspecified,
            )
            Text(
                text = model.targetRangeLabel,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun MacroProgressBar(
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

private fun layeredColors(
    progress0to1: Float,
    base: Color,
    onBackground: Color,
): Pair<Color, Color> {
    return when {
        progress0to1 < 1f -> {
            // First layer: macro color vs background
            base to onBackground.copy(alpha = .09f)
        }

        progress0to1 < 2f -> {
            // Second layer: red vs base
            Color(0xFFE53935) to base
        }

        progress0to1 < 3f -> {
            // Third layer: brighter red vs strong red
            Color(0xFFFF1744) to Color(0xFFE53935)
        }

        else -> {
            // Fourth layer: vivid red vs brighter red
            Color(0xFFFF5252) to Color(0xFFFF1744)
        }
    }
}

/**
 * Custom easing that mimics an overshoot interpolator (like Android Views).
 */
val OvershootInterpolatorEasing = Easing { fraction ->
    val tension = 2.0f
    val f = fraction - 1.0f
    f * f * ((tension + 1) * f + tension) + 1.0f
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun MacroProgressViewPreview() {
    val macro1 = MacroProgressItem(
        title = "Calories",
        progress0to1 = .15f,
        progressLabel = "1005kcal",
        targetRange0to1 = Range(.84f, 1f),
        targetRangeLabel = "2.1-2.2k",
        color = ExtraColors.calorieColor,
    )
    val macro2 = MacroProgressItem(
        title = "Salt",
        progress0to1 = 1.5f,
        progressLabel = "110g",
        targetRange0to1 = Range(.8095f, 1f),
        targetRangeLabel = "170-190g",
        color = ExtraColors.proteinColor,
    )
    AppTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingHalf),
        ) {
            val rowHeight = 32.dp
            Column(
                modifier = Modifier
                    .wrapContentWidth()
            ) {
                MacroTitleView(
                    modifier = Modifier
                        .height(rowHeight),
                    model = macro1,
                )
            }
            Column(
                modifier = Modifier
                    .weight(.5f)
            ) {
                MacroProgressBar(
                    modifier = Modifier
                        .height(rowHeight),
                    model = macro1,
                    rowIndex = 0,
                    totalRowCount = 2,
                )
            }
            Column(
                modifier = Modifier
                    .wrapContentWidth()
            ) {
                MacroTitleView(
                    modifier = Modifier
                        .height(rowHeight),
                    model = macro2,
                )
            }
            Column(
                modifier = Modifier
                    .weight(.5f)
            ) {
                MacroProgressBar(
                    modifier = Modifier
                        .height(rowHeight),
                    model = macro2,
                    rowIndex = 1,
                    totalRowCount = 2,
                )
            }
        }
    }
}
