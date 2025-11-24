package dev.gaborbiro.dailymacros.features.trends.views

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.core.axis.Axis
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.trends.TrendsViewModel
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

            state.datasets.forEach { dataset ->
                MacroChart(
                    dataset = dataset,
                    chartStyle = chartStyle,
                    chartScrollState = chartScrollState,
                    startAxis = startAxis,
                    showEveryXLabel = showEveryXLabel,
                )
            }
        }
    }
}

