package dev.gaborbiro.dailymacros.features.trends

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.gaborbiro.dailymacros.features.common.OVERVIEW_SCROLL_TO_EPOCH_DAY_KEY
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsScreen
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsViewModel
import dev.gaborbiro.dailymacros.features.trends.model.Timescale
import dev.gaborbiro.dailymacros.features.trends.model.TrendsUiState
import dev.gaborbiro.dailymacros.features.trends.model.TrendsUiUpdates
import dev.gaborbiro.dailymacros.features.trends.views.TrendsView

@Composable
fun TrendsScreen(
    trendsViewModel: TrendsViewModel,
    targetsSettingsViewModel: TargetsSettingsViewModel,
    navController: NavHostController,
    initialScrollEpochDay: Long? = null,
    initialTimescale: String? = null,
) {
    val parsedInitialTimescale = remember(initialTimescale) {
        initialTimescale?.let { runCatching { Timescale.valueOf(it) }.getOrNull() }
    }

    LaunchedEffect(trendsViewModel, initialScrollEpochDay, parsedInitialTimescale) {
        if (initialScrollEpochDay != null && parsedInitialTimescale != null) {
            trendsViewModel.onInitialScrollRequested(initialScrollEpochDay, parsedInitialTimescale)
        }
    }

    LaunchedEffect(trendsViewModel) {
        trendsViewModel.uiUpdates.collect { event ->
            when (event) {
                TrendsUiUpdates.NavigateBack -> navController.popBackStack()
                is TrendsUiUpdates.NavigateToOverviewDate -> {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(OVERVIEW_SCROLL_TO_EPOCH_DAY_KEY, event.epochDay)
                    navController.popBackStack()
                }
            }
        }
    }

    val state: TrendsUiState by trendsViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.showTargetsSettings) {
        if (state.showTargetsSettings) {
            targetsSettingsViewModel.reloadFromRepository()
        }
    }

    TrendsView(
        viewState = state,
        onTimescaleSelected = trendsViewModel::onTimescaleSelected,
        onBackNavigate = trendsViewModel::onBackNavigate,
        onSettingsActionButtonClicked = trendsViewModel::onSettingsActionButtonClicked,
        onSettingsCloseRequested = trendsViewModel::onSettingsCloseRequested,
        onSettingsAggregationModeChanged = trendsViewModel::onAggregationModeChanged,
        onSettingsThresholdChanged = trendsViewModel::onAggregationThresholdChanged,
        onTargetsSettingTapped = trendsViewModel::onDailyTargetsFromTrendsSettingsTapped,
        onGetInsightsTapped = trendsViewModel::onGetInsightsTapped,
        onGetOngoingInsightsTapped = trendsViewModel::onGetOngoingInsightsTapped,
        onDataPointTapped = trendsViewModel::onChartDataPointTapped,
        initialTimescale = parsedInitialTimescale ?: Timescale.DAYS,
        onChartScrollHandled = trendsViewModel::onChartScrollHandled,
    )

    if (state.showTargetsSettings) {
        TargetsSettingsScreen(
            viewModel = targetsSettingsViewModel,
            onCloseRequested = trendsViewModel::onTargetsSettingsCloseRequested,
        )
    }
}
