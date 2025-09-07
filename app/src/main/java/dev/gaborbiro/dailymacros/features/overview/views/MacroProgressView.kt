package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import android.util.Range
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.DailyMacrosColors
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroProgressView(
    modifier: Modifier,
    model: MacroProgressItem,
) {
    Column(
        modifier = modifier
            .padding(horizontal = PaddingHalf)
            .padding(top = 7.dp),
    ) {
        val (barColor, trackColor) = layeredColors(
            base = model.color,
            progress0to1 = model.progress0to1,
        )

        RangedProgressBar(
            modifier = Modifier.Companion
                .fillMaxWidth(),
            progress0to1 = model.progress0to1,
            min0to1 = model.targetRange0to1.lower,
            max0to1 = model.targetRange0to1.upper,
            progressColor = barColor,
            trackColor = trackColor,
        )
        Row {
            Text(
                modifier = Modifier.Companion
                    .weight(1f),
                text = model.progressLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (model.progress0to1 >= 1f) Color.Companion.Red else Color.Companion.Unspecified,
            )
            Text(
                modifier = Modifier.Companion,
                text = model.rangeLabel,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun RangedProgressBar(
    modifier: Modifier = Modifier.Companion,
    progress0to1: Float,
    min0to1: Float,
    max0to1: Float,
    progressColor: Color,
    trackColor: Color,
    barHeight: Dp = 6.dp,
) {
    BoxWithConstraints(
        modifier = modifier
            .height(barHeight)
    ) {
        val barWidthPx = constraints.maxWidth.toFloat()
        val density = LocalDensity.current

        val startOffset = with(density) { (barWidthPx * min0to1).toDp() }
        val rangeWidth = with(density) { (barWidthPx * (max0to1 - min0to1)).toDp() }

        if (progress0to1 <= 1f) {
            Box(
                Modifier.Companion
                    .offset(x = startOffset)
                    .width(rangeWidth)
                    .height(barHeight)
                    .clip(endRoundedShape(barHeight))
                    .background(
                        color = Color.Companion.Green.copy(alpha = 0.2f),
                    )
            )
        }

        // Base progress bar
        LinearProgressIndicator(
            modifier = Modifier.Companion
                .matchParentSize(),
            progress = { layeredProgress(progress0to1) },
            color = progressColor,
            trackColor = trackColor,
            strokeCap = StrokeCap.Companion.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

private fun endRoundedShape(radius: Dp): Shape =
    RoundedCornerShape(
        topStart = 0.dp,
        bottomStart = 0.dp,
        topEnd = radius,
        bottomEnd = radius
    )

@Composable
private fun layeredColors(base: Color, progress0to1: Float): Pair<Color, Color> {
    return when {
        progress0to1 < 1f -> {
            // First layer: macro pastel vs background
            base to MaterialTheme.colorScheme.onBackground.copy(alpha = .09f)
        }

        progress0to1 < 2f -> {
            // Second layer: red vs pastel
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

private fun layeredProgress(progress0to1: Float) = progress0to1 % 1f

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun MacroProgressViewPreview() {
    val macro1 = MacroProgressItem(
        title = "Calories",
        progress0to1 = .15f,
        progressLabel = "1005 cal",
        targetRange0to1 = Range(.84f, 1f),
        rangeLabel = "2.1-2.2kcal",
        color = DailyMacrosColors.calorieColor,
    )
    val macro2 = MacroProgressItem(
        title = "Protein",
        progress0to1 = 1.5f,
        progressLabel = "110g",
        targetRange0to1 = Range(.8095f, 1f),
        rangeLabel = "170-190g",
        color = DailyMacrosColors.proteinColor,
    )
    DailyMacrosTheme {
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
                MacroProgressView(
                    modifier = Modifier
                        .height(rowHeight),
                    model = macro1,
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
                MacroProgressView(
                    modifier = Modifier
                        .height(rowHeight),
                    model = macro2,
                )
            }
        }
    }
}
