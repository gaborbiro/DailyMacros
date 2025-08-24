package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import android.util.Range
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.DailyMacrosColors
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressTableUIModel
import java.time.LocalDate

@Composable
fun ListItemMacroTable(
    modifier: Modifier = Modifier,
    model: MacroProgressTableUIModel,
) {
    // Pre-split once; if model.macros identity doesn't change, this won't re-run
    val rows = remember(model.macros) { model.macros.chunked(3) }

    // Precompute paints once
    val progressColor = remember { Color.Blue.copy(alpha = 0.5f) }
    val cardColor = DailyMacrosColors.CardColorLight
    val rangeBrush = remember {
        Brush.horizontalGradient(
            listOf(
                Color.Magenta.copy(alpha = 0.15f),
                Color.Magenta.copy(alpha = 0.40f)
            )
        )
    }

    BoxWithConstraints(modifier) {
        val cellW = maxWidth / 3

        Column {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { macro ->
                        MacroCell(
                            modifier = Modifier.requiredWidth(cellW),
                            macro = macro,
                            cardColor = cardColor,
                            rangeBrush = rangeBrush,
                            progressColor = progressColor
                        )
                    }
                    // pad if row isn't full (keeps grid alignment)
                    repeat(3 - row.size) {
                        Spacer(Modifier.requiredWidth(cellW))
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroCell(
    modifier: Modifier,
    macro: MacroProgressItem,
    cardColor: Color,
    rangeBrush: Brush,
    progressColor: Color,
) {
    val linePx = with(LocalDensity.current) { 1.dp.toPx() }
    val minHeight = 48.dp

    val lower = macro.range.lower.coerceIn(0f, 1f)
    val upper = macro.range.upper.coerceIn(lower, 1f)
    val progress = macro.progress.coerceIn(0f, 1f)

    Box(
        modifier
            .heightIn(min = minHeight)
            .drawBehind {
                val w = size.width
                val h = size.height

                val borderPx = 2.dp.toPx()
                val linePx = 3.dp.toPx()

                // background
                drawRect(color = cardColor)

                // range band (clamped)
                val lower = macro.range.lower.coerceIn(0f, 1f)
                val upper = macro.range.upper.coerceIn(lower, 1f)
                val bandL = (lower * w).coerceIn(0f, w)
                val bandR = (upper * w).coerceIn(0f, w)
                val bandW = (bandR - bandL).coerceAtLeast(0f)
                if (bandW > 0f) {
                    drawRect(brush = rangeBrush, topLeft = Offset(bandL, 0f), size = Size(bandW, h))
                }

                // progress line: keep fully inside [0, w]
                val half = linePx / 2f
                val x = (macro.progress.coerceIn(0f, 1f) * w).coerceIn(half, w - half)

                // draw the line AFTER bg/band but BEFORE border, or keep it inset so border won't cover it
                drawLine(
                    color = progressColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = linePx
                )

                // border last
                drawRect(
                    color = Color.White,
                    size = Size(w, h),
                    style = Stroke(width = borderPx)
                )
            }
            .padding(8.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = macro.title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${macro.progressLabel} / ${macro.rangeLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun MacroGoalsViewPreview() {
    DailyMacrosTheme {
        ListItemMacroTable(
            modifier = Modifier
                .wrapContentHeight(),
            model = MacroProgressTableUIModel(
                date = LocalDate.now(),
                macros = listOf(
                    MacroProgressItem(
                        title = "Calories",
                        progress = .15f,
                        progressLabel = "1005 cal",
                        range = Range(.84f, .88f),
                        rangeLabel = "2.1-2.2kcal",
                    ),
                    MacroProgressItem(
                        title = "Protein",
                        progress = .0809f,
                        progressLabel = "110g",
                        range = Range(.8095f, .9047f),
                        rangeLabel = "170-190g",
                    ),
                    MacroProgressItem(
                        title = "Fat",
                        progress = .2121f,
                        progressLabel = "30g",
                        range = Range(.6818f, .9091f),
                        rangeLabel = "45-60g",
                    ),
                    MacroProgressItem(
                        title = "Carbs",
                        progress = .1818f,
                        progressLabel = "105g",
                        range = Range(.6818f, .9091f),
                        rangeLabel = "150-200g",
                    ),
                    MacroProgressItem(
                        title = "Sugar",
                        progress = .2955f,
                        progressLabel = "35g",
                        range = Range(.9091f, .9091f),
                        rangeLabel = "<40g ttl., <25g",
                    ),
                    MacroProgressItem(
                        title = "Salt",
                        progress = .0f,
                        progressLabel = "0g",
                        range = Range(.9091f, .9091f),
                        rangeLabel = "<5g (≈2g Na)",
                    ),
                    MacroProgressItem(
                        title = "Fibre",
                        progress = .0f,
                        progressLabel = "0g",
                        range = Range(.9091f, .9091f),
                        rangeLabel = "30-38g",
                    )
                )
            )
        )
    }
}
