package dev.gaborbiro.dailymacros.features.dashboard.views

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.marker.MarkerComponent
import com.patrykandpatrick.vico.core.component.text.textComponent
import com.patrykandpatrick.vico.core.dimensions.MutableDimensions
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import dev.gaborbiro.dailymacros.design.PaddingDefault
import kotlin.math.roundToInt
import kotlin.random.Random

enum class TimeScale { DAYS, WEEKS, MONTHS }

data class MacroDataPoint(val label: String, val value: Float)
data class MacroDataset(val name: String, val color: Color, val data: List<MacroDataPoint>)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MacroDashboardScreen() {
    var scale by remember { mutableStateOf(TimeScale.DAYS) }
    val macros = remember(scale) { generateDummyMacros(scale) }
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
            // scale selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScaleButton("Days", scale == TimeScale.DAYS) { scale = TimeScale.DAYS }
                ScaleButton("Weeks", scale == TimeScale.WEEKS) { scale = TimeScale.WEEKS }
                ScaleButton("Months", scale == TimeScale.MONTHS) { scale = TimeScale.MONTHS }
            }

            // charts
            macros.forEach { macro ->
                MacroChartItem(macro = macro, chartStyle = chartStyle)
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
    chartStyle: ChartStyle
) {
    // create chart data
    val producer = remember(macro) {
        ChartEntryModelProducer(
            macro.data.mapIndexed { index, point ->
                object : ChartEntry {
                    override val x = index.toFloat()
                    override val y = point.value
                    override fun withY(y: Float): ChartEntry = this
                }
            }
        )
    }

    val avg = macro.data.map { it.value }.average().toFloat()
    val min = macro.data.minOf { it.value }
    val max = macro.data.maxOf { it.value }

    // --- manual marker creation ---
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
        guideline = null // or create a visible guideline component
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = macro.name,
                style = MaterialTheme.typography.titleMedium,
                color = macro.color
            )
            Text(
                "Avg ${"%.1f".format(avg)}  Min ${"%.1f".format(min)}  Max ${"%.1f".format(max)}",
                style = MaterialTheme.typography.labelSmall
            )
        }

        // chart
        AnimatedContent(
            targetState = producer,
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { state ->
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
                    model = state.getModel()!!,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { x, _ ->
                            macro.data.getOrNull(x.roundToInt())?.label ?: ""
                        }
                    ),
                    marker = marker,
                )
            }
        }
    }
}

fun generateDummyMacros(scale: TimeScale): List<MacroDataset> {
    val labels = when (scale) {
        TimeScale.DAYS -> (1..30).map { if (it % 7 == 0) "$it/11" else it.toString() }
        TimeScale.WEEKS -> (1..12).map { "W$it" }
        TimeScale.MONTHS -> listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
    }

    fun randomSeries(name: String, color: Color, base: Float, spread: Float) =
        MacroDataset(name, color, labels.map {
            MacroDataPoint(it, base + Random.nextFloat() * spread)
        })

    return listOf(
        randomSeries("Calories (kcal)", Color(0xFF8AB4F8), 2000f, 500f),
        randomSeries("Protein (g)", Color(0xFF81C995), 100f, 40f),
        randomSeries("Carbs (g)", Color(0xFFFFC278), 250f, 60f),
        randomSeries("Fat (g)", Color(0xFFFFA6A6), 70f, 25f)
    )
}