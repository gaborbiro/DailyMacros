package dev.gaborbiro.dailymacros.ui.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.design.darkExtraColorScheme
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel

/**
 * Two-row macro summary matching the overview meal list layout.
 */
@Composable
fun CompactMacroNutrientsGrid(
    nutrients: NutrientsUiModel,
    modifier: Modifier = Modifier,
) {
    val colors = darkExtraColorScheme
    Column(modifier = modifier) {
        MacroRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            MacroPill(text = nutrients.calories.orEmpty(), bg = colors.caloriesColor, protectEndOfText = false)
            MacroPill(text = nutrients.protein.orEmpty(), bg = colors.proteinColor)
            MacroPill(text = nutrients.fat.orEmpty(), bg = colors.fatColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        MacroRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            MacroPill(text = nutrients.carbs.orEmpty(), bg = colors.carbsColor)
            MacroPill(text = nutrients.salt.orEmpty(), bg = colors.saltColor)
            MacroPill(text = nutrients.fibre.orEmpty(), bg = colors.fibreColor)
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
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = PaddingQuarter, vertical = 2.dp),
            text = text,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = if (protectEndOfText) TextOverflow.StartEllipsis else TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun MacroRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 4.dp,
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
