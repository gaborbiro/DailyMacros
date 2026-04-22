package dev.gaborbiro.dailymacros.features.trends

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsScreen
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsViewModel
import dev.gaborbiro.dailymacros.features.trends.model.TrendsUiState
import dev.gaborbiro.dailymacros.features.trends.model.TrendsUiUpdates
import dev.gaborbiro.dailymacros.features.trends.views.TrendsView

@Composable
fun TrendsScreen(
    trendsViewModel: TrendsViewModel,
    targetsSettingsViewModel: TargetsSettingsViewModel,
    navController: NavHostController,
) {
    LaunchedEffect(trendsViewModel) {
        trendsViewModel.uiUpdates.collect { event ->
            when (event) {
                TrendsUiUpdates.NavigateBack -> navController.popBackStack()
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
    )

    if (state.showTargetsSettings) {
        TargetsSettingsScreen(
            viewModel = targetsSettingsViewModel,
            onCloseRequested = trendsViewModel::onTargetsSettingsCloseRequested,
        )
    }
}
