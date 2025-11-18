package dev.gaborbiro.dailymacros.features.dashboard

import androidx.compose.runtime.Composable
import dev.gaborbiro.dailymacros.features.dashboard.views.MacroDashboardScreen


@Composable
internal fun DashboardScreen(
    viewModel: MacroDashboardViewModel,
) {
    MacroDashboardScreen(viewModel)
}
