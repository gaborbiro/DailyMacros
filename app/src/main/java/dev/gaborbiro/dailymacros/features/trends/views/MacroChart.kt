package dev.gaborbiro.dailymacros.features.trends.views

import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.ChartScrollState
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.style.ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.AxisRenderer
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.marker.MarkerComponent
import com.patrykandpatrick.vico.core.component.text.textComponent
import com.patrykandpatrick.vico.core.dimensions.MutableDimensions
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.marker.MarkerLabelFormatter
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import dev.gaborbiro.dailymacros.features.trends.model.MacroChartData
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun MacroChart(
    modifier: Modifier = Modifier,
    chartData: MacroChartData,
    chartStyle: ChartStyle,
    chartScrollState: ChartScrollState,
    startAxis: AxisRenderer<AxisPosition.Vertical.Start>,
    showEveryXLabel: Int,
) {
    val producer = remember(chartData.datasets) {
        ChartEntryModelProducer(
            chartData.datasets.flatMap { dataset ->
                listOf(
                    dataset.set.mapNotNull { point ->
                        point.value?.let { value ->
                            object : ChartEntry {
                                override val x = point.index.toFloat()
                                override val y = value.toFloat()
                                override fun withY(y: Float) = this
                            }
                        }
                    },

                    dataset.now
                        ?.takeIf { it.value != null }
                        ?.let { now ->
                            listOfNotNull(
                                dataset.set.lastOrNull { it.value != null }?.let { prev ->
                                    object : ChartEntry {
                                        override val x = prev.index.toFloat()
                                        override val y = prev.value!!.toFloat()
                                        override fun withY(y: Float) = this
                                    }
                                },
                                object : ChartEntry {
                                    override val x = now.index.toFloat()
                                    override val y = now.value!!.toFloat()
                                    override fun withY(y: Float) = this
                                }
                            )
                        }
                        ?: emptyList()
                )
            }
        )
    }

    val lineSpecs = chartData.datasets.flatMap { dataset ->
        listOf(
            // Main line
            LineChart.LineSpec(
                lineColor = dataset.color.toArgb(),
                lineThicknessDp = 2f,
                point = shapeComponent(
                    color = dataset.color,
                    shape = CircleShape
                )
            ),

            // "Now" overlay
            LineChart.LineSpec(
                lineColor = dataset.color.copy(alpha = 0.5f).toArgb(),
                lineThicknessDp = 2f,
                point = shapeComponent(
                    color = dataset.color.copy(alpha = 0.5f),
                    shape = CircleShape
                )
            )
        )
    }

    val averages = chartData.datasets.associateBy(
        keySelector = { it.name },
        valueTransform = { dataset ->
            val nonNullValues = dataset.set.mapNotNull { it.value }
            nonNullValues.takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0
        }
    )

    val marker = MarkerComponent(
        label = textComponent {
            color = MaterialTheme.colorScheme.onSurface.toArgb()
            background = shapeComponent(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = CircleShape,
                strokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                strokeWidth = 1.dp,
            )
            padding = MutableDimensions(horizontalDp = 8.dp.value, verticalDp = 4.dp.value)
        },
        indicator = shapeComponent(
            color = MaterialTheme.colorScheme.onSurface,
            shape = CircleShape,
        ),
        guideline = lineComponent(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            thickness = 1.dp
        ),
    )
    marker.labelFormatter = MarkerLabelFormatter { markedEntries, _ ->
        // Preserve line order (series index)
        val values = markedEntries
            .sortedBy { it.index }
            .distinctBy { it.entry.x * 1_000_000 + it.entry.y }
            .map { it.entry.y }

        values.joinToString(
            separator = "; ",
            prefix = "",
            postfix = ""
        ) { value ->
            "%.2f".format(value)
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dataset.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = dataset.color
                )
                Text(
                    "Average: ${averages[dataset.name]}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        ProvideChartStyle(chartStyle) {
            Chart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(top = 4.dp),
                chart = lineChart(lines = lineSpecs),
                model = producer.getModel()!!,
                marker = marker,
                startAxis = startAxis,
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { x, _ ->
                        chartData.datasets
                            .firstOrNull()
                            ?.set
                            ?.getOrNull(x.roundToInt())
                            ?.label
                            ?: ""
                    },
                    itemPlacer = remember(showEveryXLabel) {
                        AxisItemPlacer.Horizontal.default(spacing = showEveryXLabel)
                    }
                ),
                chartScrollSpec = rememberChartScrollSpec(
                    initialScroll = InitialScroll.End
                ),
                chartScrollState = chartScrollState,
                isZoomEnabled = false,
            )
        }
    }
}
