package dev.gaborbiro.dailymacros.features.trends

import androidx.compose.runtime.Composable
import dev.gaborbiro.dailymacros.features.trends.views.TrendsView


@Composable
internal fun TrendsScreen(
    viewModel: TrendsViewModel,
) {
    TrendsView(viewModel)
}
