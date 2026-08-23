package dev.gaborbiro.dailymacros.features.onboarding

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import dev.gaborbiro.dailymacros.features.common.ONBOARDING_ROUTE
import dev.gaborbiro.dailymacros.features.common.OVERVIEW_ROUTE
import dev.gaborbiro.dailymacros.features.onboarding.views.OnboardingView

@Composable
fun OnboardingScreen(
    navController: NavHostController,
    onAddWidget: () -> Unit = {},
    onRestoreFromCloud: () -> Unit = {},
    restoreFromCloudInProgress: Boolean = false,
    onRestoreFromLocalBackup: () -> Unit = {},
    restoreFromLocalBackupInProgress: Boolean = false,
    onStartGoalsQuestionnaireTapped: () -> Unit = {},
) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val context = LocalContext.current

    fun finishOnboarding() {
        if (navController.previousBackStackEntry == null) {
            // Onboarding was the start destination (real first run): replace it with Overview.
            navController.navigate(OVERVIEW_ROUTE) {
                popUpTo(ONBOARDING_ROUTE) { inclusive = true }
            }
        } else {
            // Reached from the debug menu: just return to whatever screen launched it.
            navController.popBackStack()
        }
    }

    OnboardingView(
        onAddWidget = onAddWidget,
        onRestoreFromCloud = onRestoreFromCloud,
        restoreFromCloudInProgress = restoreFromCloudInProgress,
        onRestoreFromLocalBackup = onRestoreFromLocalBackup,
        restoreFromLocalBackupInProgress = restoreFromLocalBackupInProgress,
        onStartGoalsQuestionnaireTapped = onStartGoalsQuestionnaireTapped,
        onStartTrialTapped = {
            context.findActivity()?.let(viewModel::onStartTrialTapped)
            finishOnboarding()
        },
        onSkipTapped = {
            viewModel.onSkipTapped()
            finishOnboarding()
        },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
