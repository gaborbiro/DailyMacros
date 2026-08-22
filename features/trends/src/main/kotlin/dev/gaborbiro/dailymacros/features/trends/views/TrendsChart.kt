package dev.gaborbiro.dailymacros.features.trends.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.VicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import dev.gaborbiro.dailymacros.features.trends.R
import dev.gaborbiro.dailymacros.features.trends.model.ChartDataPoint
import dev.gaborbiro.dailymacros.features.trends.model.Timescale
import dev.gaborbiro.dailymacros.features.trends.model.TrendsChartUiModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun TrendsChart(
    modifier: Modifier = Modifier,
    chartData: TrendsChartUiModel,
    scrollState: VicoScrollState,
    showEveryXLabel: Int,
    timescale: Timescale,
    onScrollToDateConfirmed: (epochDay: Long) -> Unit = {},
    /** Shows this point's marker as if the user had tapped it - e.g. when Trends is opened
     *  from a daily/weekly summary tap in Overview - without the "Scroll to <date>" pill,
     *  since there's nowhere further to navigate to from here. Uses Vico's persistentMarkers,
     *  which bypasses the tap-driven markerController/markerVisibilityListener entirely, so it
     *  doesn't touch tappedPointEpochDay below - and, left alone, would stay shown forever
     *  regardless of any real tap. showPersistentHighlight is the bridge that lets an actual
     *  interactive tap (anywhere on the chart) dismiss it. */
    highlightedIndex: Int? = null,
) {
    val verticalItemPlacer = remember(chartData.pinnedMaxY) {
        if (chartData.pinnedMaxY != null) VerticalAxis.ItemPlacer.count(count = { 4 })
        else VerticalAxis.ItemPlacer.step()
    }
    val startAxis = VerticalAxis.rememberStart(
        size = BaseAxis.Size.Fixed(50.dp),
        valueFormatter = CartesianValueFormatter { _, value, _ ->
            value.roundToInt().toString()
        },
        itemPlacer = verticalItemPlacer,
    )
    val modelProducer = remember { CartesianChartModelProducer() }

    val segmentsByDataset = remember(chartData.datasets) {
        chartData.datasets.map { dataset ->
            val segments = buildList {
                val current = mutableListOf<ChartDataPoint>()
                for (point in dataset.set) {
                    if (point.value != null) {
                        current.add(point)
                    } else if (current.isNotEmpty()) {
                        add(current.toList())
                        current.clear()
                    }
                }
                if (current.isNotEmpty()) add(current.toList())
            }
            dataset to segments
        }
    }

    LaunchedEffect(segmentsByDataset, chartData.datasets) {
        val maxXIndex = chartData.datasets.maxOf { ds ->
            maxOf(
                ds.set.maxOfOrNull { it.index } ?: 0,
                ds.current?.index ?: 0,
            )
        }.toDouble().coerceAtLeast(1.0)

        modelProducer.runTransaction {
            lineSeries {
                for ((dataset, segments) in segmentsByDataset) {
                    if (segments.isNotEmpty()) {
                        for (segment in segments) {
                            series(
                                x = segment.map { it.index.toDouble() },
                                y = segment.map { it.value!! },
                            )
                        }
                    } else {
                        series(y = listOf(0))
                    }

                    val currentPoint = dataset.current?.takeIf { it.value != null }
                    val lastHistorical = dataset.set.lastOrNull { it.value != null }
                    if (currentPoint != null && lastHistorical != null) {
                        series(
                            x = listOf(lastHistorical.index.toDouble(), currentPoint.index.toDouble()),
                            y = listOf(lastHistorical.value!!, currentPoint.value!!),
                        )
                    } else {
                        series(y = listOf(0))
                    }
                }
                for (dataset in chartData.datasets) {
                    dataset.targetMaxY?.let { y ->
                        series(
                            x = listOf(0.0, maxXIndex),
                            y = listOf(y, y),
                        )
                    }
                    dataset.targetMinY?.let { y ->
                        if (y != dataset.targetMaxY) {
                            series(
                                x = listOf(0.0, maxXIndex),
                                y = listOf(y, y),
                            )
                        }
                    }
                }
            }
        }
    }

    val lines = remember(segmentsByDataset, chartData.datasets) {
        val dataLineThickness = 2.dp
        val dataLines = segmentsByDataset.flatMap { (dataset, segments) ->
            val historicalLine = LineCartesianLayer.Line(
                fill = LineCartesianLayer.LineFill.single(Fill(dataset.color)),
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = dataLineThickness),
                pointProvider = LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(
                        component = ShapeComponent(Fill(dataset.color), CircleShape),
                    )
                ),
            )
            val currentLine = LineCartesianLayer.Line(
                fill = LineCartesianLayer.LineFill.single(Fill(dataset.color.copy(alpha = 0.5f))),
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = dataLineThickness),
                pointProvider = LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(
                        component = ShapeComponent(
                            Fill(dataset.color.copy(alpha = 0.5f)),
                            CircleShape,
                        ),
                    )
                ),
            )
            val segmentLines = if (segments.isNotEmpty()) {
                List(segments.size) { historicalLine }
            } else {
                listOf(historicalLine)
            }
            segmentLines + currentLine
        }
        fun dashedTargetLine(color: Color) = LineCartesianLayer.Line(
            fill = LineCartesianLayer.LineFill.single(Fill(color.copy(alpha = 0.88f))),
            areaFill = null,
            stroke = LineCartesianLayer.LineStroke.Dashed(
                thickness = dataLineThickness,
                dashLength = 6.dp,
                gapLength = 4.dp,
            ),
        )
        val targetLines = chartData.datasets.flatMap { d ->
            buildList {
                if (d.targetMaxY != null) add(dashedTargetLine(d.color))
                if (d.targetMinY != null && d.targetMinY != d.targetMaxY) add(dashedTargetLine(d.color))
            }
        }
        dataLines + targetLines
    }

    val lineProvider = remember(lines) {
        LineCartesianLayer.LineProvider.series(lines)
    }

    val rangeProvider = remember(chartData.pinnedMaxY) {
        val pinnedMax = chartData.pinnedMaxY
        if (pinnedMax != null) {
            object : CartesianLayerRangeProvider {
                override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore) = minY
                override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore) = pinnedMax
            }
        } else {
            CartesianLayerRangeProvider.auto()
        }
    }

    val targetYValues = remember(chartData.datasets) {
        buildSet {
            chartData.datasets.forEach { d ->
                d.targetMaxY?.let { add(it) }
                d.targetMinY?.takeIf { it != d.targetMaxY }?.let { add(it) }
            }
        }
    }

    val markerValueFormatter = remember(targetYValues) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val values = targets
                .filterIsInstance<LineCartesianLayerMarkerTarget>()
                .flatMap { it.points }
                .map { it.entry.y }
                .filter { y -> y !in targetYValues }
                .distinctBy { "%.2f".format(it) }

            values.joinToString(separator = "; ") { value ->
                "%.2f".format(value)
            }
        }
    }

    val indicatorFactory = remember<(androidx.compose.ui.graphics.Color) -> ShapeComponent> {
        { color -> ShapeComponent(Fill(color), CircleShape) }
    }

    val marker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            style = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface),
            background = rememberShapeComponent(
                fill = Fill(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                shape = CircleShape,
                strokeFill = Fill(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)),
                strokeThickness = 1.dp,
            ),
            padding = Insets(horizontal = 8.dp, vertical = 4.dp),
        ),
        valueFormatter = markerValueFormatter,
        indicator = indicatorFactory,
        guideline = rememberLineComponent(
            fill = Fill(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            thickness = 1.dp,
        ),
    )

    val bottomAxisValueFormatter = remember(chartData.datasets) {
        val dataset = chartData.datasets.firstOrNull()
        val labelByIndex = buildMap {
            dataset?.set?.forEach { put(it.index, it.label) }
            dataset?.current?.let { put(it.index, it.label) }
        }
        CartesianValueFormatter { _, value, _ ->
            labelByIndex[value.roundToInt()] ?: value.roundToInt().toString()
        }
    }

    val itemPlacer = remember(showEveryXLabel) {
        HorizontalAxis.ItemPlacer.aligned(spacing = { showEveryXLabel })
    }

    val zoomState = remember {
        VicoZoomState(
            zoomEnabled = false,
            initialZoom = Zoom.fixed(),
            minZoom = Zoom.fixed(),
            maxZoom = Zoom.fixed(),
        )
    }

    // Tapping a point shows its value marker as before, and also resolves to a real date shown
    // in a "Scroll to <date>" pill below the chart - tapping THAT (not the dot itself) is what
    // actually navigates to Overview, so a tap only previews the value like it always did and
    // never jumps you away by accident. rememberToggleOnTap distinguishes a discrete tap from a
    // scrub/drag gesture, so dragging across the chart to preview values doesn't also surface it.
    var tappedPointEpochDay by remember(chartData) { mutableStateOf<Long?>(null) }
    // Whether the programmatic highlight (see highlightedIndex/persistentMarkers below) is
    // still showing. persistentMarkers bypasses markerController/markerVisibilityListener
    // entirely - it's not part of the tap-toggle system at all - so without this, no tap
    // (on that dot, on a different one, anywhere) could ever make it go away. Any real
    // interactive marker event means the user has started tapping around, so that's the
    // signal to drop the programmatic one and let the normal toggle system take over fully.
    var showPersistentHighlight by remember(chartData, highlightedIndex) { mutableStateOf(highlightedIndex != null) }
    val markerController = CartesianMarkerController.rememberToggleOnTap()
    val markerVisibilityListener = remember(chartData) {
        fun resolveEpochDay(targets: List<CartesianMarker.Target>): Long? {
            val index = targets.firstOrNull()?.x?.roundToInt() ?: return null
            return chartData.datasets.firstNotNullOfOrNull { dataset ->
                dataset.set.firstOrNull { it.index == index }?.epochDay
                    ?: dataset.current?.takeIf { it.index == index }?.epochDay
            }
        }
        object : CartesianMarkerVisibilityListener {
            override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                showPersistentHighlight = false
                tappedPointEpochDay = resolveEpochDay(targets)
            }

            // With ToggleOnTap, tapping a different point while the marker is already shown
            // updates it in place rather than hiding and re-showing it - onShown alone missed
            // that transition, which is why the pill's date used to get stuck on the first tap.
            override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                showPersistentHighlight = false
                tappedPointEpochDay = resolveEpochDay(targets)
            }

            override fun onHidden(marker: CartesianMarker) {
                showPersistentHighlight = false
                tappedPointEpochDay = null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        chartData.datasets.forEach { dataset ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dataset.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = dataset.color
                )
            }
        }

        CartesianChartHost(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(top = 4.dp),
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(lineProvider = lineProvider, rangeProvider = rangeProvider),
                startAxis = startAxis,
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = bottomAxisValueFormatter,
                    itemPlacer = itemPlacer,
                ),
                marker = marker,
                markerVisibilityListener = markerVisibilityListener,
                markerController = markerController,
                persistentMarkers = {
                    if (showPersistentHighlight && highlightedIndex != null) marker at highlightedIndex.toDouble()
                },
            ),
            modelProducer = modelProducer,
            scrollState = scrollState,
            zoomState = zoomState,
            animationSpec = null,
            animateIn = false,
        )

        tappedPointEpochDay?.let { epochDay ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = stringResource(
                            R.string.trends_content_scroll_to_date,
                            formatScrollToDateLabel(epochDay, timescale),
                        ),
                        modifier = Modifier
                            .clickable { onScrollToDateConfirmed(epochDay) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
        }
    }
}

private val SCROLL_TO_DATE_DAY_FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
private val SCROLL_TO_DATE_WEEK_START_FORMATTER = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val SCROLL_TO_DATE_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

private fun formatScrollToDateLabel(epochDay: Long, timescale: Timescale): String {
    val date = LocalDate.ofEpochDay(epochDay)
    return when (timescale) {
        Timescale.DAYS -> date.format(SCROLL_TO_DATE_DAY_FORMATTER)
        Timescale.WEEKS -> "week of ${date.format(SCROLL_TO_DATE_WEEK_START_FORMATTER)}"
        Timescale.MONTHS -> date.format(SCROLL_TO_DATE_MONTH_FORMATTER)
    }
}
