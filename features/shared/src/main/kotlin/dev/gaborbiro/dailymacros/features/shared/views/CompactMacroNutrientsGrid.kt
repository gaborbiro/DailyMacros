package dev.gaborbiro.dailymacros.features.shared.views

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.darkExtraColorScheme
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel
import kotlin.math.floor

/**
 * Macro summary for list rows and dialogs: equal-width cells per row using [MacroRow].
 * Column count is derived from available width (1–3) so narrow rows get wider cells and less truncation.
 */
@Composable
fun CompactMacroNutrientsGrid(
    nutrients: NutrientsUiModel,
    modifier: Modifier = Modifier,
) {
    val colors = darkExtraColorScheme
    val rowSpacing = 8.dp
    val cellSpacing = 8.dp
    /** When the row is narrower than ~3 cells at this width, drop to 2 or 1 columns. */
    val minCellWidth = 88.dp

    val slots = listOf(
        Triple(nutrients.calories.orEmpty(), colors.caloriesColor, false),
        Triple(nutrients.protein.orEmpty(), colors.proteinColor, true),
        Triple(nutrients.fat.orEmpty(), colors.fatColor, true),
        Triple(nutrients.carbs.orEmpty(), colors.carbsColor, true),
        Triple(nutrients.salt.orEmpty(), colors.saltColor, true),
        Triple(nutrients.fibre.orEmpty(), colors.fibreColor, true),
    )

    BoxWithConstraints(modifier = modifier) {
        val columns = remember(maxWidth) {
            val ratio = (maxWidth + cellSpacing) / (minCellWidth + cellSpacing)
            floor(ratio).toInt().coerceIn(1, 3)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            slots.chunked(columns).forEachIndexed { index, chunk ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(rowSpacing))
                }
                MacroRow(
                    modifier = Modifier.fillMaxWidth(),
                    spacing = cellSpacing,
                ) {
                    chunk.forEach { (text, bg, protect) ->
                        MacroPill(
                            text = text,
                            bg = bg,
                            protectEndOfText = protect,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroPill(
    text: String,
    bg: Color,
    color: Color = Color.Black,
    protectEndOfText: Boolean = true,
) {
    if (text.isBlank()) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
        )
        return
    }

    Card(
        modifier = Modifier,
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 4.dp),
            text = text,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = if (protectEndOfText) TextOverflow.StartEllipsis else TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun MacroRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val spacingPx = with(LocalDensity.current) { spacing.roundToPx() }

    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->

        val n = measurables.size
        val maxWidth = constraints.maxWidth
        if (n == 0) return@Layout layout(maxWidth, 0) {}

        val totalSpacing = spacingPx * (n - 1)
        val available = (maxWidth - totalSpacing).coerceAtLeast(0)
        val equal = available / n

        val required = measurables.map {
            it.maxIntrinsicWidth(constraints.maxHeight)
        }

        val widths = IntArray(n) { equal }

        var deficit = 0
        for (i in 0 until n) {
            val need = required[i] - equal
            if (need > 0) deficit += need
        }

        var pool = 0
        for (i in 0 until n) {
            if (required[i] < equal) {
                pool += equal - required[i]
            }
        }

        val transferable = minOf(deficit, pool)

        if (transferable > 0) {
            val shrinkables = (0 until n).filter { required[it] < equal }

            var remaining = transferable
            for (i in shrinkables) {
                val maxShrink = equal - required[i]
                val take = minOf(maxShrink, remaining)
                widths[i] -= take
                remaining -= take
                if (remaining == 0) break
            }

            val growables = (0 until n).filter { required[it] > equal }
            var give = transferable
            for (i in growables) {
                val need = required[i] - equal
                val add = minOf(need, give)
                widths[i] += add
                give -= add
                if (give == 0) break
            }
        }

        val placeables = measurables.mapIndexed { i, m ->
            m.measure(Constraints.fixedWidth(widths[i]))
        }

        val height = placeables.maxOf { it.height }

        layout(maxWidth, height) {
            var x = 0
            for (i in 0 until n) {
                val p = placeables[i]
                p.placeRelative(x, 0)
                x += p.width + if (i != n - 1) spacingPx else 0
            }
        }
    }
}
