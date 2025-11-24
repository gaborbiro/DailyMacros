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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.ChartScrollState
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
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
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import dev.gaborbiro.dailymacros.features.trends.model.MacroChartDataset
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun MacroChart(
    dataset: MacroChartDataset,
    chartStyle: ChartStyle,
    chartScrollState: ChartScrollState,
    startAxis: AxisRenderer<AxisPosition.Vertical.Start>,
    showEveryXLabel: Int,
) {
    val producer = remember(dataset) {
        ChartEntryModelProducer(
            dataset.set.mapNotNull { point ->
                point.value?.let { value ->
                    object : ChartEntry {
                        override val x = point.index.toFloat()
                        override val y = value
                        override fun withY(y: Float): ChartEntry = this
                    }
                }
            },
            dataset.now
                ?.takeIf { it.value != null }
                ?.let { point ->
                    listOfNotNull(
                        dataset.set.lastOrNull { it.value != null }?.let { point ->
                            object : ChartEntry {
                                override val x = point.index.toFloat()
                                override val y = point.value!!
                                override fun withY(y: Float): ChartEntry = this
                            }
                        },
                        object : ChartEntry {
                            override val x = point.index.toFloat()
                            override val y = point.value!!
                            override fun withY(y: Float): ChartEntry = this
                        }
                    )
                }
                ?: emptyList()
        )
    }

    val nonNullValues = dataset.set.mapNotNull { it.value }
    val avg = nonNullValues.takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0

    val marker = MarkerComponent(
        label = textComponent {
            color = Color.White.toArgb()
            background = shapeComponent(
                color = Color.Black.copy(alpha = 0.7f),
                shape = CircleShape
            )
            padding = MutableDimensions(horizontalDp = 8.dp.value, verticalDp = 4.dp.value)
        },
        indicator = shapeComponent(
            color = dataset.color,
            shape = CircleShape
        ),
        guideline = null,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
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
                "Average: $avg",
                style = MaterialTheme.typography.labelSmall
            )
        }

        ProvideChartStyle(chartStyle) {
            Chart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(top = 4.dp),
                chart = lineChart(
                    lines = listOf(
                        LineChart.LineSpec(
                            lineColor = dataset.color.toArgb(),
                            point = shapeComponent(
                                color = dataset.color,
                                shape = CircleShape
                            )
                        ),
                        LineChart.LineSpec(
                            lineColor = dataset.color.copy(alpha = .5f).toArgb(),
                            point = shapeComponent(
                                color = dataset.color.copy(alpha = .5f),
                                shape = CircleShape
                            )
                        )
                    )
                ),
                model = producer.getModel()!!,
                startAxis = startAxis,
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { x, _ ->
                        dataset.set.getOrNull(x.roundToInt())?.label ?: ""
                    },
                    itemPlacer = remember(showEveryXLabel) { AxisItemPlacer.Horizontal.default(spacing = showEveryXLabel) }
                ),
                marker = marker,
                chartScrollSpec = rememberChartScrollSpec(initialScroll = InitialScroll.End),
                chartScrollState = chartScrollState,
                isZoomEnabled = false,
            )
        }
    }
}
