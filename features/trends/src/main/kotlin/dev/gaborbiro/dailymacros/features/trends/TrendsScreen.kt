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
    viewModel: TrendsViewModel,
    targetsViewModel: TargetsSettingsViewModel,
    navController: NavHostController,
) {
    LaunchedEffect(viewModel) {
        viewModel.uiUpdates.collect { event ->
            when (event) {
                TrendsUiUpdates.NavigateBack -> navController.popBackStack()
            }
        }
    }

    val state: TrendsUiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.showTargetsSettings) {
        if (state.showTargetsSettings) {
            targetsViewModel.reloadFromRepository()
        }
    }

    TrendsView(
        viewState = state,
        onTimescaleSelected = viewModel::onTimescaleSelected,
        onBackNavigate = viewModel::onBackNavigate,
        onSettingsActionButtonClicked = viewModel::onSettingsActionButtonClicked,
        onSettingsCloseRequested = viewModel::onSettingsCloseRequested,
        onSettingsAggregationModeChanged = viewModel::onAggregationModeChanged,
        onSettingsThresholdChanged = viewModel::onAggregationThresholdChanged,
        onEditTargetsFromChartsTapped = viewModel::onEditTargetsFromChartsTapped,
    )

    if (state.showTargetsSettings) {
        TargetsSettingsScreen(
            viewModel = targetsViewModel,
            onCloseRequested = viewModel::onTargetsSettingsCloseRequested,
        )
    }
}
