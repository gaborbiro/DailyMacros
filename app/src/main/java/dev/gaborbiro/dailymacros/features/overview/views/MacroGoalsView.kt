package dev.gaborbiro.dailymacros.features.overview.views

import android.util.Range
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.DailyMacrosColors
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.features.overview.model.GoalCellItem
import dev.gaborbiro.dailymacros.features.overview.model.MacroGoalsProgress

@Composable
fun MacroGoalsView(model: MacroGoalsProgress) {
    val matrix = arrayOf(
        arrayOf(
            model.calories, model.protein, model.fat,
        ),
        arrayOf(
            model.carbs, model.sugar, model.salt,
        )
    )

    EqualTable(
        rows = 2,
        cols = 3,
    ) { r, c ->
        val cellData = matrix[r][c]
        Box(
            Modifier
                .border(1.dp, DailyMacrosColors.DailyMacrosGrey80)
                .background(color = DailyMacrosColors.CardColorLight)
        ) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(cellData.range.lower.coerceAtLeast(.0001f)))
                Box(
                    modifier = Modifier
                        .weight((cellData.range.upper - cellData.range.lower).coerceAtLeast(.0001f))
                        .fillMaxHeight()
                        .background(Color.Magenta.copy(alpha = .15f)),
                )
                Spacer(Modifier.weight((1 - cellData.range.upper).coerceAtLeast(.0001f)))
            }
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight((cellData.progress).coerceAtLeast(.0001f)))
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.Blue.copy(alpha = .5f)),
                )
                Spacer(Modifier.weight((1 - cellData.progress).coerceAtLeast(.0001f)))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.Center)
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.End),
                    color = Color.Black,
                    text = cellData.rangeLabel,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp),
                ) {
                    Text(
                        color = Color.Black,
                        text = cellData.title,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        modifier = Modifier
                            .weight(1f),
                        color = Color.Blue.copy(alpha = .5f),
                        text = cellData.value,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun MacroGoalsViewPreview() {
    DailyMacrosTheme {
        MacroGoalsView(
            MacroGoalsProgress(
                calories = GoalCellItem(
                    title = "Calories",
                    value = "1005 cal",
                    rangeLabel = "2.1-2.2kcal",
                    range = Range(.84f, .88f),
                    progress = .15f,
                ),
                protein = GoalCellItem(
                    title = "Protein",
                    value = "110g",
                    rangeLabel = "170-190g",
                    range = Range(.8095f, .9047f),
                    progress = .0809f,
                ),
                fat = GoalCellItem(
                    title = "Fat",
                    value = "30g",
                    rangeLabel = "45-60g",
                    range = Range(.6818f, .9091f),
                    progress = .2121f,
                ),
                carbs = GoalCellItem(
                    title = "Carbs",
                    value = "105g",
                    rangeLabel = "150-200g",
                    range = Range(.6818f, .9091f),
                    progress = .1818f,
                ),
                sugar = GoalCellItem(
                    title = "Sugar",
                    value = "35g",
                    rangeLabel = "<40g ttl., <25g",
                    range = Range(.9091f, .9091f),
                    progress = .2955f,
                ),
                salt = GoalCellItem(
                    title = "Salt",
                    value = "0g",
                    rangeLabel = "<5g (≈2g Na)",
                    range = Range(.9091f, .9091f),
                    progress = .0f,
                ),
            )
        )
    }
}

@Composable
fun EqualTable(
    modifier: Modifier = Modifier,
    rows: Int,
    cols: Int,
    cell: @Composable (row: Int, col: Int) -> Unit,
) = SubcomposeLayout(modifier) { constraints ->
    require(rows > 0 && cols > 0)
    val count = rows * cols

    // Probe pass: learn natural sizes (one measure per measurable)
    val loose = constraints.copy(minWidth = 0, minHeight = 0)
    val probe = (0 until count).map { i ->
        subcompose("probe_$i") { cell(i / cols, i % cols) }
            .first()
            .measure(loose)
    }
    val maxW = probe.maxOf { it.width }
    val maxH = probe.maxOf { it.height }

    val hasBoundedW = constraints.maxWidth != Constraints.Infinity
    val hasBoundedH = constraints.maxHeight != Constraints.Infinity

    // If bounded: fill evenly. If unbounded: use content-driven max sizes.
    val cellW = if (hasBoundedW) constraints.maxWidth / cols else maxW
    val cellH = if (hasBoundedH) constraints.maxHeight / rows else maxH

    val fixed = Constraints.fixed(cellW, cellH)

    // Final pass: fresh subcomposition, single measure per measurable
    val placeables = (0 until count).map { i ->
        subcompose("final_$i") { cell(i / cols, i % cols) }
            .first()
            .measure(fixed)
    }

    // Layout size (note: integer division may leave a tiny unused gutter)
    val layoutW =
        (cellW * cols).coerceIn(constraints.minWidth, if (hasBoundedW) constraints.maxWidth else Int.MAX_VALUE)
    val layoutH =
        (cellH * rows).coerceIn(constraints.minHeight, if (hasBoundedH) constraints.maxHeight else Int.MAX_VALUE)

    layout(layoutW, layoutH) {
        placeables.forEachIndexed { i, p ->
            val r = i / cols
            val c = i % cols
            p.place(x = c * cellW, y = r * cellH)
        }
    }
}
