package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints

@Composable
internal fun EqualTable(
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
