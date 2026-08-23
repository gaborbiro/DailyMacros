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
