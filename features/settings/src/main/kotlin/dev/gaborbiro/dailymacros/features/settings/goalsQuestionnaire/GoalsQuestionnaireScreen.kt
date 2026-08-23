package dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.GoalsQuestionnaireUiUpdates
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.views.GoalsQuestionnaireView

@Composable
fun GoalsQuestionnaireScreen(
    viewModel: GoalsQuestionnaireViewModel,
    onCloseRequested: () -> Unit,
) {
    val viewState by viewModel.uiState.collectAsStateWithLifecycle()

    GoalsQuestionnaireView(
        viewState = viewState,
        onGoalSelected = viewModel::onGoalSelected,
        onGenderSelected = viewModel::onGenderSelected,
        onAgeBracketSelected = viewModel::onAgeBracketSelected,
        onActivityLevelSelected = viewModel::onActivityLevelSelected,
        onDietaryFocusToggled = viewModel::onDietaryFocusToggled,
        onDietaryFocusNoneTapped = viewModel::onDietaryFocusNoneTapped,
        onPresetTargetChanged = viewModel::onPresetTargetChanged,
        onAcceptTapped = viewModel::onAcceptTapped,
        onCloseTapped = onCloseRequested,
    )

    LaunchedEffect(viewModel) {
        viewModel.uiUpdates.collect { event ->
            when (event) {
                GoalsQuestionnaireUiUpdates.Finished -> onCloseRequested()
            }
        }
    }
}
