package dev.gaborbiro.dailymacros.features.trends.views

import android.content.res.Configuration
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.views.PreviewContext
import dev.gaborbiro.dailymacros.features.trends.model.ChartDataPoint
import dev.gaborbiro.dailymacros.features.trends.model.ChartDataset
import dev.gaborbiro.dailymacros.features.trends.model.DailyAggregationMode
import dev.gaborbiro.dailymacros.features.trends.model.Timescale
import dev.gaborbiro.dailymacros.features.trends.model.TrendsChartUiModel
import dev.gaborbiro.dailymacros.features.trends.model.TrendsSettingsUIModel
import dev.gaborbiro.dailymacros.features.trends.model.TrendsViewState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt


@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun TrendsView(
    viewState: TrendsViewState,
    onTimescaleSelected: (scale: Timescale) -> Unit,
    onBackNavigate: () -> Unit,
    onSettingsActionButtonClicked: () -> Unit,
    onSettingsCloseRequested: () -> Unit,
    onSettingsAggregationModeChanged: (DailyAggregationMode, Timescale) -> Unit,
    onSettingsThresholdChanged: (Long, Timescale) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        topBar = {
            TopAppBar(
                title = { Text("Trends") },
                navigationIcon = {
                    IconButton(onClick = onBackNavigate) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back Button"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSettingsActionButtonClicked,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        var timescale by remember {
            mutableStateOf(Timescale.DAYS)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScaleButton(
                    label = "Days",
                    selected = timescale == Timescale.DAYS,
                    onClick = {
                        onTimescaleSelected(Timescale.DAYS)
                        timescale = Timescale.DAYS
                    },
                    modifier = Modifier.weight(1f),
                )
                ScaleButton(
                    label = "Weeks",
                    selected = timescale == Timescale.WEEKS,
                    onClick = {
                        onTimescaleSelected(Timescale.WEEKS)
                        timescale = Timescale.WEEKS
                    },
                    modifier = Modifier.weight(1f),
                )
                ScaleButton(
                    label = "Months",
                    selected = timescale == Timescale.MONTHS,
                    onClick = {
                        onTimescaleSelected(Timescale.MONTHS)
                        timescale = Timescale.MONTHS
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            val startAxis = VerticalAxis.rememberStart(
                size = BaseAxis.Size.Fixed(50.dp),
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    value.roundToInt().toString()
                },
            )

            val showEveryXLabel = when (timescale) {
                Timescale.WEEKS -> 2
                else -> 1
            }

            key(timescale) {
                var chartsVisible by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(300)
                    chartsVisible = true
                }

                if (chartsVisible) {
                    val scrollState = rememberVicoScrollState(
                        initialScroll = Scroll.Absolute.End,
                    )

                    viewState.charts.forEach { chartData ->
                        TrendsChart(
                            modifier = Modifier
                                .padding(start = PaddingDefault),
                            chartData = chartData,
                            scrollState = scrollState,
                            startAxis = startAxis,
                            showEveryXLabel = showEveryXLabel,
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        if (viewState.settings is TrendsSettingsUIModel.Show) {
            TrendsSettingsBottomSheet(
                dailyAggregationMode = viewState.settings.dailyAggregationMode,
                qualifiedDaysThreshold = viewState.settings.qualifiedDaysThreshold,
                onDismissRequested = onSettingsCloseRequested,
                onAggregationModeChanged = { onSettingsAggregationModeChanged(it, timescale) },
                onThresholdChanged = { onSettingsThresholdChanged(it, timescale) },
            )
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TrendsViewPreview() {
    PreviewContext {
        TrendsView(
            viewState = TrendsViewState(
                charts = previewData,
            ),
            onTimescaleSelected = {},
            onBackNavigate = {},
            onSettingsActionButtonClicked = {},
            onSettingsCloseRequested = {},
            onSettingsAggregationModeChanged = { _, _ -> },
            onSettingsThresholdChanged = { _, _ -> },
        )
    }
}

private val previewData = listOf(
    TrendsChartUiModel(
        datasets = listOf(
            ChartDataset(
                name = "Chart",
                color = androidx.compose.ui.graphics.Color.Blue,
                set = listOf(
                    ChartDataPoint(1, "test", 1.0),
                    ChartDataPoint(2, "test", 2.0)
                ),
                current = ChartDataPoint(3, "test", 3.0),
            ),
        )
    ),
    TrendsChartUiModel(
        datasets = listOf(
            ChartDataset(
                name = "Chart2",
                color = androidx.compose.ui.graphics.Color.Red,
                set = listOf(
                    ChartDataPoint(1, "test", 3.0),
                    ChartDataPoint(2, "test", 2.0)
                ),
                current = ChartDataPoint(3, "test", 1.0),
            )
        )
    )
)
