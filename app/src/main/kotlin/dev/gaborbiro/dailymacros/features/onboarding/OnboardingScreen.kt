package dev.gaborbiro.dailymacros.features.onboarding

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.gaborbiro.dailymacros.features.common.ONBOARDING_ROUTE
import dev.gaborbiro.dailymacros.features.common.OVERVIEW_ROUTE
import dev.gaborbiro.dailymacros.features.onboarding.views.OnboardingView
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.GoalsQuestionnaireViewModel

@Composable
fun OnboardingScreen(
    navController: NavHostController,
) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    // Scoped to this screen's own nav back-stack entry, independent of the GoalsQuestionnaireViewModel
    // instance used by the standalone questionnaire screen reachable from Daily Targets.
    val goalsQuestionnaireViewModel: GoalsQuestionnaireViewModel = hiltViewModel()
    val goalsQuestionnaireUiState by goalsQuestionnaireViewModel.uiState.collectAsStateWithLifecycle()
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
        goalsAnswers = goalsQuestionnaireUiState.answers,
        goalsPresetTargets = goalsQuestionnaireUiState.presetTargets,
        onGoalSelected = goalsQuestionnaireViewModel::onGoalSelected,
        onGenderSelected = goalsQuestionnaireViewModel::onGenderSelected,
        onAgeBracketSelected = goalsQuestionnaireViewModel::onAgeBracketSelected,
        onActivityLevelSelected = goalsQuestionnaireViewModel::onActivityLevelSelected,
        onDietaryFocusToggled = goalsQuestionnaireViewModel::onDietaryFocusToggled,
        onDietaryFocusNoneTapped = goalsQuestionnaireViewModel::onDietaryFocusNoneTapped,
        onPresetTargetChanged = goalsQuestionnaireViewModel::onPresetTargetChanged,
        onPersistGoalsPresetTargets = { goalsQuestionnaireViewModel.persistPresetTargets() },
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
