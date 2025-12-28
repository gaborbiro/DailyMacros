package dev.gaborbiro.dailymacros.features.trends

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.trends.model.TrendsViewState
import dev.gaborbiro.dailymacros.features.trends.views.TrendsView


@Composable
internal fun TrendsScreen(
    viewModel: TrendsViewModel,
) {
    val state: TrendsViewState by viewModel.viewState.collectAsStateWithLifecycle()
    TrendsView(
        viewState = state,
        onTimeScaleSelected = viewModel::onTimeScaleSelected,
        onBackNavigate = viewModel::onBackNavigate,
        onSettingsActionButtonClicked = viewModel::onSettingsActionButtonClicked,
        onSettingsCloseRequested = viewModel::onSettingsCloseRequested,
        onSettingsAggregationModeChanged = viewModel::onAggregationModeChanged,
        onSettingsThresholdChanged = viewModel::onAggregationThresholdChanged,
    )
}
