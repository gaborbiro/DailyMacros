package dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model

import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetUiModel

enum class Goal {
    LOSE_WEIGHT,
    BUILD_MUSCLE,
    EAT_HEALTHIER,
    JUST_TRACK,
}

enum class Gender {
    MALE,
    FEMALE,
}

enum class AgeBracket {
    UNDER_25,
    AGE_25_40,
    AGE_40_60,
    AGE_60_PLUS,
}

enum class ActivityLevel {
    SEDENTARY,
    LIGHT,
    ACTIVE,
    VERY_ACTIVE,
}

enum class DietaryFocus {
    SUGAR,
    SALT,
    SATURATED_FAT,
    FIBRE,
}

data class GoalsQuestionnaireAnswers(
    val goal: Goal? = null,
    val gender: Gender? = null,
    val ageBracket: AgeBracket? = null,
    val activityLevel: ActivityLevel? = null,
    val dietaryFocus: Set<DietaryFocus> = emptySet(),
    /** Set once the user has interacted with the dietary-focus page in any way (toggled a focus
     *  on/off, or explicitly tapped "None of these") - lets that page require one explicit tap
     *  before advancing, the same as every other page, while still allowing "no focuses" as a
     *  valid answer. */
    val dietaryFocusReviewed: Boolean = false,
) {
    val isComplete: Boolean
        get() = goal != null && gender != null && ageBracket != null && activityLevel != null
}

data class GoalsQuestionnaireUiState(
    val answers: GoalsQuestionnaireAnswers = GoalsQuestionnaireAnswers(),
    val presetTargets: Map<MacroType, TargetUiModel> = emptyMap(),
)

sealed class GoalsQuestionnaireUiUpdates {
    data object Finished : GoalsQuestionnaireUiUpdates()
}

/** Number of pages in the questionnaire itself (goal, gender, age, activity, dietary focus,
 *  results) - shared by the standalone screen (reachable from Settings) and by onboarding, which
 *  flattens these same pages into its own pager starting from its 2nd page. */
const val GOALS_QUESTIONNAIRE_PAGE_COUNT = 6

/**
 * Whether the user can move on from a given page of the questionnaire, keyed by the page's index
 * *within the questionnaire itself* (0 = goal, ..., 5 = results) - independent of where those
 * pages are hosted (the standalone screen, or flattened into onboarding), so both hosts gate
 * their own "Next" button identically.
 */
fun canAdvanceFromQuestionnairePage(
    localPage: Int,
    answers: GoalsQuestionnaireAnswers,
    presetTargets: Map<MacroType, TargetUiModel>,
): Boolean = when (localPage) {
    0 -> answers.goal != null
    1 -> answers.gender != null
    2 -> answers.ageBracket != null
    3 -> answers.activityLevel != null
    4 -> answers.dietaryFocusReviewed
    else -> presetTargets.isNotEmpty()
}
