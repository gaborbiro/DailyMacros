package dev.gaborbiro.dailymacros.features.dashboard.views

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.core.axis.AxisRenderer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.ChartScrollState
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.Axis
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.marker.MarkerComponent
import com.patrykandpatrick.vico.core.component.text.textComponent
import com.patrykandpatrick.vico.core.dimensions.MutableDimensions
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.dashboard.MacroDashboardViewModel
import dev.gaborbiro.dailymacros.features.dashboard.model.MacroDataset
import dev.gaborbiro.dailymacros.features.dashboard.model.TimeScale
import kotlin.math.roundToInt


data class MacroDataPoint(val label: String, val value: Float?)

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun MacroDashboardScreen(
    viewModel: MacroDashboardViewModel,
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val chartStyle = m3ChartStyle()

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(start = PaddingDefault)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScaleButton("Days", state.scale == TimeScale.DAYS) { viewModel.onScaleSelected(TimeScale.DAYS) }
                ScaleButton("Weeks", state.scale == TimeScale.WEEKS) { viewModel.onScaleSelected(TimeScale.WEEKS) }
                ScaleButton("Months", state.scale == TimeScale.MONTHS) { viewModel.onScaleSelected(TimeScale.MONTHS) }
            }

            val daysChartScrollState = rememberChartScrollState()
            val weeksChartScrollState = rememberChartScrollState()
            val monthsChartScrollState = rememberChartScrollState()

            val chartScrollState = when (state.scale) {
                TimeScale.DAYS -> daysChartScrollState
                TimeScale.WEEKS -> weeksChartScrollState
                TimeScale.MONTHS -> monthsChartScrollState
            }

            // Shared start axis within a tab so all charts line up vertically.
            val startAxis = rememberStartAxis(sizeConstraint = Axis.SizeConstraint.Exact(70f))

            state.datasets.forEach { macro ->
                MacroChartItem(
                    macro = macro,
                    chartStyle = chartStyle,
                    chartScrollState = chartScrollState,
                    startAxis = startAxis,
                )
            }
        }
    }
}

@Composable
fun ScaleButton(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.height(36.dp)
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MacroChartItem(
    macro: MacroDataset,
    chartStyle: ChartStyle,
    chartScrollState: ChartScrollState,
    startAxis: AxisRenderer<AxisPosition.Vertical.Start>,
) {
    val producer = remember(macro) {
        ChartEntryModelProducer(
            macro.data.mapIndexedNotNull { index, point ->
                point.value?.let { value ->
                    object : ChartEntry {
                        override val x = index.toFloat()
                        override val y = value
                        override fun withY(y: Float): ChartEntry = this
                    }
                }
            }
        )
    }

    val nonNullValues = macro.data.mapNotNull { it.value }
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
            color = macro.color,
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
                text = macro.name,
                style = MaterialTheme.typography.titleMedium,
                color = macro.color
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
                            lineColor = macro.color.toArgb(),
                            point = shapeComponent(
                                color = macro.color,
                                shape = CircleShape
                            )
                        )
                    )
                ),
                model = producer.getModel()!!,
                startAxis = startAxis,
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { x, _ ->
                        macro.data.getOrNull(x.roundToInt())?.label ?: ""
                    }
                ),
                marker = marker,
                chartScrollSpec = rememberChartScrollSpec(initialScroll = InitialScroll.End),
                chartScrollState = chartScrollState,
            )
        }
    }
}
