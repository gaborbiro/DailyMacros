package dev.gaborbiro.dailymacros.features.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.gaborbiro.dailymacros.features.paywall.views.PaywallView

@Composable
fun PaywallScreen(
    navController: NavHostController,
) {
    val viewModel: PaywallViewModel = hiltViewModel()
    val context = LocalContext.current
    val subscriptionState by viewModel.subscriptionState.collectAsStateWithLifecycle()

    PaywallView(
        subscriptionState = subscriptionState,
        onBackNavigateRequested = { navController.popBackStack() },
        onSubscribeTapped = { context.findActivity()?.let(viewModel::onSubscribeTapped) },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
