package dev.gaborbiro.dailymacros.features.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.gaborbiro.dailymacros.features.common.OVERVIEW_SCROLL_TO_EPOCH_DAY_KEY
import dev.gaborbiro.dailymacros.features.common.PAYWALL_ROUTE
import dev.gaborbiro.dailymacros.features.common.SETTINGS_ROUTE
import dev.gaborbiro.dailymacros.features.common.SETTINGS_ROUTE_PATTERN
import dev.gaborbiro.dailymacros.features.common.TRENDS_ROUTE
import dev.gaborbiro.dailymacros.features.common.TRENDS_SCROLL_EPOCH_DAY_ARG
import dev.gaborbiro.dailymacros.features.common.TRENDS_TIMESCALE_ARG
import dev.gaborbiro.dailymacros.features.overview.model.OverviewUiUpdates
import dev.gaborbiro.dailymacros.features.overview.views.OverviewView
import dev.gaborbiro.dailymacros.features.shared.ModalNavigator

@Composable
fun OverviewScreen(
    modalNavigator: ModalNavigator,
    navController: NavHostController,
    onAddWidget: () -> Unit = {},
) {
    val viewModel: OverviewViewModel = hiltViewModel()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.uiUpdates.collect { event ->
            when (event) {
                is OverviewUiUpdates.EditRecord -> modalNavigator.launchViewRecordDetails(context, event.recordId)
                is OverviewUiUpdates.ViewImage -> modalNavigator.launchToShowRecordImage(context, event.recordId)
                OverviewUiUpdates.OpenSettingsScreen -> navController.navigate(SETTINGS_ROUTE) {
                    launchSingleTop = true
                    popUpTo(SETTINGS_ROUTE_PATTERN) { inclusive = true }
                }
                is OverviewUiUpdates.OpenTrendsScreen -> {
                    val query = if (event.scrollToEpochDay != null && event.timescale != null) {
                        "?$TRENDS_SCROLL_EPOCH_DAY_ARG=${event.scrollToEpochDay}&$TRENDS_TIMESCALE_ARG=${event.timescale}"
                    } else {
                        ""
                    }
                    navController.navigate("$TRENDS_ROUTE$query")
                }
                OverviewUiUpdates.OpenPaywallScreen -> navController.navigate(PAYWALL_ROUTE)
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.onSearchTermChanged(search = null)
    }

    // Set by TrendsScreen (via OVERVIEW_SCROLL_TO_EPOCH_DAY_KEY) just before popping back to
    // this screen, when the user tapped a Trends chart point - see Navigation.kt for why this
    // goes through the back-stack entry's SavedStateHandle rather than a nav route argument.
    val scrollRequestHandle = navController.currentBackStackEntry?.savedStateHandle
    val requestedScrollEpochDay by scrollRequestHandle
        ?.getStateFlow<Long?>(OVERVIEW_SCROLL_TO_EPOCH_DAY_KEY, null)
        ?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }
    LaunchedEffect(requestedScrollEpochDay) {
        requestedScrollEpochDay?.let { epochDay ->
            viewModel.onScrollToDateRequested(epochDay)
            scrollRequestHandle?.remove<Long>(OVERVIEW_SCROLL_TO_EPOCH_DAY_KEY)
        }
    }

    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    OverviewView(
        viewState = viewState,
        onRepeatMenuItemTapped = viewModel::onRepeatMenuItemTapped,
        onAnalyseMacrosMenuItemTapped = viewModel::onAnalyseMacrosMenuItemTapped,
        onDeleteMenuItemTapped = viewModel::onDeleteMenuItemTapped,
        onRecordImageTapped = viewModel::onRecordImageTapped,
        onRecordBodyTapped = viewModel::onRecordBodyTapped,
        onUndoDeleteTapped = viewModel::onUndoDeleteTapped,
        onUndoDeleteDismissed = viewModel::onUndoDeleteDismissed,
        onUndoDeleteSnackbarShown = viewModel::onUndoDeleteSnackbarShown,
        onSearchTermChanged = viewModel::onSearchTermChanged,
        onSettingsButtonTapped = viewModel::onSettingsButtonTapped,
        onSubscribeBannerTapped = viewModel::onSubscribeBannerTapped,
        onSubscribeBannerDismissed = viewModel::onSubscribeBannerDismissed,
        onAddWidget = onAddWidget,
        onDailySummaryTapped = viewModel::onDailySummaryTapped,
        onWeeklySummaryTapped = viewModel::onWeeklySummaryTapped,
        onLoadMore = viewModel::onLoadMore,
        onScrollHandled = viewModel::onScrollHandled,
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.finalizePendingUndos()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
