package dev.gaborbiro.dailymacros.features.trends

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.trends.model.TrendsUiState
import dev.gaborbiro.dailymacros.features.trends.views.TrendsView


@Composable
internal fun TrendsScreen(
    viewModel: TrendsViewModel,
) {
    val state: TrendsUiState by viewModel.uiState.collectAsStateWithLifecycle()

    TrendsView(
        viewState = state,
        onTimescaleSelected = viewModel::onTimescaleSelected,
        onBackNavigate = viewModel::onBackNavigate,
        onSettingsActionButtonClicked = viewModel::onSettingsActionButtonClicked,
        onSettingsCloseRequested = viewModel::onSettingsCloseRequested,
        onSettingsAggregationModeChanged = viewModel::onAggregationModeChanged,
        onSettingsThresholdChanged = viewModel::onAggregationThresholdChanged,
    )
}
