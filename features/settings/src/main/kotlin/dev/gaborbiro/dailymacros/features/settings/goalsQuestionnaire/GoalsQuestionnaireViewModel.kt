package dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.ActivityLevel
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.AgeBracket
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.DietaryFocus
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.Gender
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.Goal
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.GoalsQuestionnaireAnswers
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.GoalsQuestionnaireUiState
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.GoalsQuestionnaireUiUpdates
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsUiMapper
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetUiModel
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetsSettingsUiState
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsQuestionnaireViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val uiMapper: TargetsSettingsUiMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsQuestionnaireUiState())
    val uiState: StateFlow<GoalsQuestionnaireUiState> = _uiState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<GoalsQuestionnaireUiUpdates>()
    val uiUpdates = _uiUpdates.asSharedFlow()

    fun onGoalSelected(goal: Goal) {
        updateAnswersAndRecompute { it.copy(goal = goal) }
    }

    fun onGenderSelected(gender: Gender) {
        updateAnswersAndRecompute { it.copy(gender = gender) }
    }

    fun onAgeBracketSelected(ageBracket: AgeBracket) {
        updateAnswersAndRecompute { it.copy(ageBracket = ageBracket) }
    }

    fun onActivityLevelSelected(activityLevel: ActivityLevel) {
        updateAnswersAndRecompute { it.copy(activityLevel = activityLevel) }
    }

    fun onDietaryFocusToggled(focus: DietaryFocus) {
        updateAnswersAndRecompute { answers ->
            val focusSet = if (focus in answers.dietaryFocus) {
                answers.dietaryFocus - focus
            } else {
                answers.dietaryFocus + focus
            }
            answers.copy(dietaryFocus = focusSet, dietaryFocusReviewed = true)
        }
    }

    fun onDietaryFocusNoneTapped() {
        updateAnswersAndRecompute { answers ->
            answers.copy(dietaryFocus = emptySet(), dietaryFocusReviewed = true)
        }
    }

    fun onPresetTargetChanged(type: MacroType, target: TargetUiModel) {
        _uiState.update {
            it.copy(presetTargets = it.presetTargets + (type to target))
        }
    }

    fun onAcceptTapped() {
        if (!persistPresetTargets()) return
        viewModelScope.launch {
            _uiUpdates.emit(GoalsQuestionnaireUiUpdates.Finished)
        }
    }

    /**
     * Persists the current preset targets to the repository, returning false (no-op) if there's
     * nothing to save yet - e.g. the user swiped straight to the results page without completing
     * the required questions first. Exposed separately from [onAcceptTapped] so onboarding, which
     * flattens these same pages into its own pager instead of hosting this screen, can save and
     * move on to its next page without going through this screen's own "finished" event.
     */
    fun persistPresetTargets(): Boolean {
        val presetTargets = _uiState.value.presetTargets
        if (presetTargets.isEmpty()) return false
        repo.setTargets(uiMapper.map(TargetsSettingsUiState(targets = presetTargets)))
        return true
    }

    private inline fun updateAnswersAndRecompute(
        transform: (GoalsQuestionnaireAnswers) -> GoalsQuestionnaireAnswers,
    ) {
        _uiState.update { state ->
            val answers = transform(state.answers)
            val presetTargets = if (answers.isComplete) {
                uiMapper.map(GoalsPresetCalculator.computeTargets(answers)).targets
            } else {
                state.presetTargets
            }
            state.copy(answers = answers, presetTargets = presetTargets)
        }
    }
}
