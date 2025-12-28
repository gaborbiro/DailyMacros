package dev.gaborbiro.dailymacros.features.trends.views

import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.core.axis.Axis
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PreviewContext
import dev.gaborbiro.dailymacros.features.common.views.PreviewImageStoreProvider
import dev.gaborbiro.dailymacros.features.trends.model.DailyAggregationMode
import dev.gaborbiro.dailymacros.features.trends.model.MacroChartData
import dev.gaborbiro.dailymacros.features.trends.model.MacroChartDataPoint
import dev.gaborbiro.dailymacros.features.trends.model.MacroChartDataset
import dev.gaborbiro.dailymacros.features.trends.model.TimeScale
import dev.gaborbiro.dailymacros.features.trends.model.TrendsSettingsUIModel
import dev.gaborbiro.dailymacros.features.trends.model.TrendsViewState
import kotlin.math.roundToInt


@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun TrendsView(
    viewState: TrendsViewState,
    onTimeScaleSelected: (scale: TimeScale) -> Unit,
    onBackNavigate: () -> Unit,
    onSettingsActionButtonClicked: () -> Unit,
    onSettingsCloseRequested: () -> Unit,
    onSettingsAggregationModeChanged: (DailyAggregationMode) -> Unit,
    onSettingsThresholdChanged: (Long) -> Unit,
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
                    selected = viewState.timeScale == TimeScale.DAYS,
                    onClick = { onTimeScaleSelected(TimeScale.DAYS) },
                    modifier = Modifier.weight(1f),
                )
                ScaleButton(
                    label = "Weeks",
                    selected = viewState.timeScale == TimeScale.WEEKS,
                    onClick = { onTimeScaleSelected(TimeScale.WEEKS) },
                    modifier = Modifier.weight(1f),
                )
                ScaleButton(
                    label = "Months",
                    selected = viewState.timeScale == TimeScale.MONTHS,
                    onClick = { onTimeScaleSelected(TimeScale.MONTHS) },
                    modifier = Modifier.weight(1f),
                )
            }

            val daysChartScrollState = rememberChartScrollState()
            val weeksChartScrollState = rememberChartScrollState()
            val monthsChartScrollState = rememberChartScrollState()

            val chartScrollState = when (viewState.timeScale) {
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

            val showEveryXLabel = when (viewState.timeScale) {
                TimeScale.WEEKS -> 2
                else -> 1
            }

            val chartStyle = m3ChartStyle()

            viewState.chartsData.forEach { chartData ->
                MacroChart(
                    modifier = Modifier
                        .padding(start = PaddingDefault),
                    chartData = chartData,
                    chartStyle = chartStyle,
                    chartScrollState = chartScrollState,
                    startAxis = startAxis,
                    showEveryXLabel = showEveryXLabel,
                )
            }
        }

        if (viewState.trendsSettings is TrendsSettingsUIModel.Show) {
            TrendsSettingsBottomSheet(
                dailyAggregationMode = viewState.trendsSettings.dailyAggregationMode,
                qualifiedDaysThreshold = viewState.trendsSettings.qualifiedDaysThreshold,
                onDismissRequested = onSettingsCloseRequested,
                onAggregationModeChanged = onSettingsAggregationModeChanged,
                onThresholdChanged = onSettingsThresholdChanged,
            )
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TrendsViewPreviewDays() {
    PreviewContext {
        PreviewImageStoreProvider {
            TrendsView(
                viewState = TrendsViewState(
                    timeScale = TimeScale.DAYS,
                    chartsData = previewData,
                ),
                onTimeScaleSelected = {},
                onBackNavigate = {},
                onSettingsActionButtonClicked = {},
                onSettingsCloseRequested = {},
                onSettingsAggregationModeChanged = {},
                onSettingsThresholdChanged = {},
            )
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TrendsViewPreviewWeeks() {
    PreviewContext {
        TrendsView(
            viewState = TrendsViewState(
                timeScale = TimeScale.WEEKS,
                chartsData = previewData,
            ),
            onTimeScaleSelected = {},
            onBackNavigate = {},
            onSettingsActionButtonClicked = {},
            onSettingsCloseRequested = {},
            onSettingsAggregationModeChanged = {},
            onSettingsThresholdChanged = {},
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TrendsViewPreviewMonths() {
    PreviewContext {
        TrendsView(
            viewState = TrendsViewState(
                timeScale = TimeScale.MONTHS,
                chartsData = previewData,
            ),
            onTimeScaleSelected = {},
            onBackNavigate = {},
            onSettingsActionButtonClicked = {},
            onSettingsCloseRequested = {},
            onSettingsAggregationModeChanged = {},
            onSettingsThresholdChanged = {},
        )
    }
}

private val previewData = listOf(
    MacroChartData(
        datasets = listOf(
            MacroChartDataset(
                name = "Chart",
                color = androidx.compose.ui.graphics.Color.Blue,
                set = listOf(
                    MacroChartDataPoint(1, "test", 1.0),
                    MacroChartDataPoint(2, "test", 2.0)
                ),
                now = MacroChartDataPoint(3, "test", 3.0),
            ),
        )
    ),
    MacroChartData(
        datasets = listOf(
            MacroChartDataset(
                name = "Chart2",
                color = androidx.compose.ui.graphics.Color.Red,
                set = listOf(
                    MacroChartDataPoint(1, "test", 3.0),
                    MacroChartDataPoint(2, "test", 2.0)
                ),
                now = MacroChartDataPoint(3, "test", 1.0),
            )
        )
    )
)