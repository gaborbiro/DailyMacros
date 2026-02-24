package dev.gaborbiro.dailymacros.features.trends.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.VicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
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
import dev.gaborbiro.dailymacros.features.trends.model.TrendsChartUiModel
import kotlin.math.roundToInt

@Composable
internal fun TrendsChart(
    modifier: Modifier = Modifier,
    chartData: TrendsChartUiModel,
    scrollState: VicoScrollState,
    startAxis: VerticalAxis<Axis.Position.Vertical.Start>,
    showEveryXLabel: Int,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(chartData.datasets) {
        modelProducer.runTransaction {
            lineSeries {
                for (dataset in chartData.datasets) {
                    val mainPoints = dataset.set.filter { it.value != null }
                    series(
                        x = mainPoints.map { it.index.toDouble() },
                        y = mainPoints.map { it.value!! },
                    )

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
            }
        }
    }

    val lines = remember(chartData.datasets) {
        chartData.datasets.flatMap { dataset ->
            listOf(
                LineCartesianLayer.Line(
                    fill = LineCartesianLayer.LineFill.single(Fill(dataset.color)),
                    stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp),
                    pointProvider = LineCartesianLayer.PointProvider.single(
                        LineCartesianLayer.Point(
                            component = ShapeComponent(Fill(dataset.color), CircleShape),
                        )
                    ),
                ),
                // current
                LineCartesianLayer.Line(
                    fill = LineCartesianLayer.LineFill.single(Fill(dataset.color.copy(alpha = 0.5f))),
                    stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp),
                    pointProvider = LineCartesianLayer.PointProvider.single(
                        LineCartesianLayer.Point(
                            component = ShapeComponent(
                                Fill(dataset.color.copy(alpha = 0.5f)),
                                CircleShape,
                            ),
                        )
                    ),
                ),
            )
        }
    }

    val lineProvider = remember(lines) {
        LineCartesianLayer.LineProvider.series(lines)
    }

    val markerValueFormatter = remember {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val values = targets
                .filterIsInstance<LineCartesianLayerMarkerTarget>()
                .flatMap { it.points }
                .distinctBy { it.entry.x * 1_000_000 + it.entry.y }
                .map { it.entry.y }

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
                rememberLineCartesianLayer(lineProvider = lineProvider),
                startAxis = startAxis,
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = bottomAxisValueFormatter,
                    itemPlacer = itemPlacer,
                ),
                marker = marker,
            ),
            modelProducer = modelProducer,
            scrollState = scrollState,
            zoomState = zoomState,
            animationSpec = null,
            animateIn = false,
        )
    }
}
