package dev.gaborbiro.dailymacros.features.trends.views

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
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
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.trends.TrendsViewModel
import dev.gaborbiro.dailymacros.features.trends.model.MacroDataset
import dev.gaborbiro.dailymacros.features.trends.model.TimeScale
import kotlin.math.roundToInt


@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun TrendsView(
    viewModel: TrendsViewModel,
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val chartStyle = m3ChartStyle()

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        topBar = {
            TopAppBar(
                title = { Text("Trends") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackNavigate() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back Button"
                        )
                    }
                },
            )
        },
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
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScaleButton(
                    label = "Days",
                    selected = state.scale == TimeScale.DAYS,
                    onClick = { viewModel.onScaleSelected(TimeScale.DAYS) },
                    modifier = Modifier.weight(1f),
                )
                ScaleButton(
                    label = "Weeks",
                    selected = state.scale == TimeScale.WEEKS,
                    onClick = { viewModel.onScaleSelected(TimeScale.WEEKS) },
                    modifier = Modifier.weight(1f),
                )
                ScaleButton(
                    label = "Months",
                    selected = state.scale == TimeScale.MONTHS,
                    onClick = { viewModel.onScaleSelected(TimeScale.MONTHS) },
                    modifier = Modifier.weight(1f),
                )
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
            val startAxis = rememberStartAxis(
                sizeConstraint = Axis.SizeConstraint.Exact(50f),
                valueFormatter = { value, _ ->
                    value.roundToInt().toString()
                }
            )

            val showEveryXLabel = when (state.scale) {
                TimeScale.WEEKS -> 2
                else -> 1
            }

            state.datasets.forEach { macro ->
                MacroChartItem(
                    macro = macro,
                    chartStyle = chartStyle,
                    chartScrollState = chartScrollState,
                    startAxis = startAxis,
                    showEveryXLabel = showEveryXLabel,
                )
            }
        }
    }
}

@Composable
private fun ScaleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        modifier = modifier
            .height(36.dp)
            .fillMaxWidth(),
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = label,
                textAlign = TextAlign.Center
            )
        },
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MacroChartItem(
    macro: MacroDataset,
    chartStyle: ChartStyle,
    chartScrollState: ChartScrollState,
    startAxis: AxisRenderer<AxisPosition.Vertical.Start>,
    showEveryXLabel: Int,
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
