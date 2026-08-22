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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import dev.gaborbiro.dailymacros.features.trends.R
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.views.PreviewContext
import dev.gaborbiro.dailymacros.features.trends.model.ChartDataPoint
import dev.gaborbiro.dailymacros.features.trends.model.ChartDataset
import dev.gaborbiro.dailymacros.features.trends.model.DayQualifier
import dev.gaborbiro.dailymacros.features.trends.model.Timescale
import dev.gaborbiro.dailymacros.features.trends.model.TrendsChartUiModel
import dev.gaborbiro.dailymacros.features.trends.model.TrendsSettingsUIModel
import dev.gaborbiro.dailymacros.features.trends.model.TrendsUiState
import kotlinx.coroutines.delay


@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun TrendsView(
    viewState: TrendsUiState,
    onTimescaleSelected: (scale: Timescale) -> Unit,
    onBackNavigate: () -> Unit,
    onSettingsActionButtonClicked: () -> Unit,
    onSettingsCloseRequested: () -> Unit,
    onSettingsAggregationModeChanged: (DayQualifier, Timescale) -> Unit,
    onSettingsThresholdChanged: (Long, Timescale) -> Unit,
    onTargetsSettingTapped: () -> Unit,
    onGetInsightsTapped: () -> Unit,
    onGetOngoingInsightsTapped: () -> Unit,
    onDataPointTapped: (epochDay: Long) -> Unit = {},
    initialTimescale: Timescale = Timescale.DAYS,
    onChartScrollHandled: () -> Unit = {},
) {
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trends_content_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackNavigate) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.trends_content_back_cd),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSettingsActionButtonClicked,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.trends_content_settings_cd),
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        var timescale by remember {
            mutableStateOf(initialTimescale)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            val hasInsufficientData = viewState.charts.isEmpty() ||
                viewState.charts.all { chart -> chart.datasets.all { it.set.isEmpty() } }

            if (hasInsufficientData) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.trends_content_empty_state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScaleButton(
                    modifier = Modifier
                        .weight(1f),
                    label = stringResource(R.string.trends_content_scale_days),
                    selected = timescale == Timescale.DAYS,
                    onClick = {
                        onTimescaleSelected(Timescale.DAYS)
                        timescale = Timescale.DAYS
                    },
                )
                ScaleButton(
                    modifier = Modifier
                        .weight(1f),
                    label = stringResource(R.string.trends_content_scale_weeks),
                    selected = timescale == Timescale.WEEKS,
                    onClick = {
                        onTimescaleSelected(Timescale.WEEKS)
                        timescale = Timescale.WEEKS
                    },
                )
                ScaleButton(
                    modifier = Modifier
                        .weight(1f),
                    label = stringResource(R.string.trends_content_scale_months),
                    selected = timescale == Timescale.MONTHS,
                    onClick = {
                        onTimescaleSelected(Timescale.MONTHS)
                        timescale = Timescale.MONTHS
                    },
                )
            }

            val showEveryXLabel = when (timescale) {
                Timescale.WEEKS -> 2
                else -> 1
            }

            if (timescale == Timescale.DAYS && viewState.aiInsightsEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.trends_content_ongoing_week_insights_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        viewState.ongoingWeekInsightsDateRange?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        viewState.ongoingInsightsFetchedAtLabel?.let {
                            Text(
                                text = stringResource(R.string.trends_content_fetched_at, it),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onGetOngoingInsightsTapped,
                        enabled = !viewState.ongoingWeekInsightsLoading,
                    ) {
                        if (viewState.ongoingWeekInsightsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(if (viewState.ongoingWeekInsights.isNullOrEmpty().not()) R.string.trends_content_refresh else R.string.trends_content_get_insights))
                        }
                    }
                }

                viewState.ongoingWeekInsightsError?.let { error ->
                    Text(
                        text = error,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (viewState.ongoingWeekInsights.isNullOrEmpty().not()) {
                    Text(
                        text = viewState.ongoingWeekInsights,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (timescale == Timescale.WEEKS && viewState.aiInsightsEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.trends_content_weekly_insights_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        viewState.weeklyInsightsDateRange?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        viewState.weeklyInsightsFetchedAtLabel?.let {
                            Text(
                                text = stringResource(R.string.trends_content_fetched_at, it),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onGetInsightsTapped,
                        enabled = !viewState.weeklyInsightsLoading,
                    ) {
                        if (viewState.weeklyInsightsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(if (viewState.weeklyInsights.isNotEmpty()) R.string.trends_content_refresh else R.string.trends_content_get_insights))
                        }
                    }
                }

                viewState.weeklyInsightsError?.let { error ->
                    Text(
                        text = error,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                viewState.weeklyInsightsWeekAssessment?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            key(timescale) {
                var chartsVisible by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(300)
                    chartsVisible = true
                }

                if (chartsVisible) {
                    // Captured once, not read reactively: Vico resolves a Scroll.Absolute value
                    // lazily, whenever it next lays out the chart, so baking the target index into
                    // initialScroll (like the pre-existing Scroll.Absolute.End default already
                    // did) lands correctly regardless of whether the underlying chart model has
                    // finished its own async transaction yet. An imperative scrollState.scroll(...)
                    // call from a separate LaunchedEffect raced that transaction instead - it could
                    // run before the model had this chart's data, silently landing nowhere.
                    val initialScrollTarget = remember { viewState.scrollToChartIndex }
                    val scrollState = rememberVicoScrollState(
                        initialScroll = initialScrollTarget
                            ?.let { Scroll.Absolute.x(it.toDouble(), bias = 0.3f) }
                            ?: Scroll.Absolute.End,
                    )

                    LaunchedEffect(Unit) {
                        if (initialScrollTarget != null) onChartScrollHandled()
                    }

                    viewState.charts.forEach { chartData ->
                        TrendsChart(
                            modifier = Modifier
                                .padding(start = PaddingDefault),
                            chartData = chartData,
                            scrollState = scrollState,
                            showEveryXLabel = showEveryXLabel,
                            timescale = timescale,
                            onScrollToDateConfirmed = onDataPointTapped,
                            // Same one-shot target as the pre-scroll above - highlights the
                            // point the user tapped in Overview as if they'd tapped it here too,
                            // without the "Scroll to <date>" pill (there's nowhere further to go).
                            highlightedIndex = initialScrollTarget,
                        )
                        if (timescale == Timescale.WEEKS && viewState.aiInsightsEnabled) {
                            viewState.weeklyInsights[chartData.title]?.let { insight ->
                                Text(
                                    text = insight,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
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
                dayQualifier = viewState.settings.dayQualifier,
                qualifiedDaysThreshold = viewState.settings.qualifiedDaysThreshold,
                onTargetsSettingTapped = onTargetsSettingTapped,
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
            viewState = TrendsUiState(
                charts = previewData,
            ),
            onTimescaleSelected = {},
            onBackNavigate = {},
            onSettingsActionButtonClicked = {},
            onSettingsCloseRequested = {},
            onSettingsAggregationModeChanged = { _, _ -> },
            onSettingsThresholdChanged = { _, _ -> },
            onTargetsSettingTapped = {},
            onGetInsightsTapped = {},
            onGetOngoingInsightsTapped = {},
        )
    }
}

private val previewData = listOf(
    TrendsChartUiModel(
        title = "Calories",
        datasets = listOf(
            ChartDataset(
                name = "Chart",
                color = androidx.compose.ui.graphics.Color.Blue,
                set = listOf(
                    ChartDataPoint(1, "test", 1.0, epochDay = 1L),
                    ChartDataPoint(2, "test", 2.0, epochDay = 2L)
                ),
                current = ChartDataPoint(3, "test", 3.0, epochDay = 3L),
            ),
        )
    ),
    TrendsChartUiModel(
        title = "Protein",
        datasets = listOf(
            ChartDataset(
                name = "Chart2",
                color = androidx.compose.ui.graphics.Color.Red,
                set = listOf(
                    ChartDataPoint(1, "test", 3.0, epochDay = 1L),
                    ChartDataPoint(2, "test", 2.0, epochDay = 2L)
                ),
                current = ChartDataPoint(3, "test", 1.0, epochDay = 3L),
            )
        )
    )
)
