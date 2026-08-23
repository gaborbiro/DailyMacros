package dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire

import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.ActivityLevel
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.AgeBracket
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.DietaryFocus
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.Gender
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.Goal
import dev.gaborbiro.dailymacros.features.settings.goalsQuestionnaire.model.GoalsQuestionnaireAnswers
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import kotlin.math.roundToInt

/**
 * Turns the questionnaire's coarse, tap-only answers into a set of daily targets. This is
 * deliberately a flat lookup + a couple of ratios, not a BMR/TDEE calculation - the questionnaire
 * is meant to hand the user a quick, editable starting point ("a vaguely acceptable ballpark"),
 * not a personalised nutrition plan. Values are rounded to numbers a person would actually see on
 * a food label rather than mathematically precise ones.
 */
object GoalsPresetCalculator {

    private val BASE_CALORIES: Map<Gender, Map<AgeBracket, Map<ActivityLevel, Int>>> = mapOf(
        Gender.FEMALE to mapOf(
            AgeBracket.UNDER_25 to activityRow(1800, 2000, 2200, 2400),
            AgeBracket.AGE_25_40 to activityRow(1800, 2000, 2200, 2400),
            AgeBracket.AGE_40_60 to activityRow(1600, 1800, 2000, 2200),
            AgeBracket.AGE_60_PLUS to activityRow(1600, 1800, 2000, 2200),
        ),
        Gender.MALE to mapOf(
            AgeBracket.UNDER_25 to activityRow(2200, 2400, 2800, 3000),
            AgeBracket.AGE_25_40 to activityRow(2200, 2400, 2600, 2800),
            AgeBracket.AGE_40_60 to activityRow(2000, 2200, 2400, 2600),
            AgeBracket.AGE_60_PLUS to activityRow(1800, 2000, 2200, 2400),
        ),
    )

    private fun activityRow(sedentary: Int, light: Int, active: Int, veryActive: Int) = mapOf(
        ActivityLevel.SEDENTARY to sedentary,
        ActivityLevel.LIGHT to light,
        ActivityLevel.ACTIVE to active,
        ActivityLevel.VERY_ACTIVE to veryActive,
    )

    private val GOAL_CALORIE_ADJUSTMENT: Map<Goal, Int> = mapOf(
        Goal.LOSE_WEIGHT to -400,
        Goal.BUILD_MUSCLE to 300,
        Goal.EAT_HEALTHIER to 0,
        Goal.JUST_TRACK to 0,
    )

    /** protein/carbs/fat fractions of total calories, each goal's summing to 1.0. */
    private val GOAL_MACRO_SPLIT: Map<Goal, Triple<Double, Double, Double>> = mapOf(
        Goal.LOSE_WEIGHT to Triple(0.35, 0.35, 0.30),
        Goal.BUILD_MUSCLE to Triple(0.30, 0.45, 0.25),
        Goal.EAT_HEALTHIER to Triple(0.20, 0.50, 0.30),
        Goal.JUST_TRACK to Triple(0.20, 0.50, 0.30),
    )

    private const val SALT_MAX_G = 6
    private const val SUGAR_MAX_G = 30
    private const val SATURATED_FAT_MAX_MALE_G = 30
    private const val SATURATED_FAT_MAX_FEMALE_G = 20
    private const val FIBRE_MIN_G = 30
    private const val FIBRE_MAX_G = 45

    fun computeTargets(answers: GoalsQuestionnaireAnswers): Targets {
        require(answers.isComplete) { "Cannot compute preset targets from incomplete answers" }
        val goal = answers.goal!!
        val gender = answers.gender!!

        val baseCalories = BASE_CALORIES.getValue(gender).getValue(answers.ageBracket!!).getValue(answers.activityLevel!!)
        val calories = baseCalories + GOAL_CALORIE_ADJUSTMENT.getValue(goal)
        val (proteinFraction, carbsFraction, fatFraction) = GOAL_MACRO_SPLIT.getValue(goal)

        val proteinGrams = (calories * proteinFraction / 4).roundToInt()
        val carbsGrams = (calories * carbsFraction / 4).roundToInt()
        val fatGrams = (calories * fatFraction / 9).roundToInt()

        val saturatedFatMax = if (gender == Gender.MALE) SATURATED_FAT_MAX_MALE_G else SATURATED_FAT_MAX_FEMALE_G

        return Targets(
            calories = rangeTarget(calories, spreadFraction = 0.1, roundTo = 50),
            protein = rangeTarget(proteinGrams, spreadFraction = 0.15, roundTo = 5),
            carbs = rangeTarget(carbsGrams, spreadFraction = 0.15, roundTo = 5),
            fat = rangeTarget(fatGrams, spreadFraction = 0.15, roundTo = 5),
            salt = capTarget(DietaryFocus.SALT in answers.dietaryFocus, SALT_MAX_G),
            fibre = floorTarget(DietaryFocus.FIBRE in answers.dietaryFocus, FIBRE_MIN_G, FIBRE_MAX_G),
            ofWhichSaturated = capTarget(DietaryFocus.SATURATED_FAT in answers.dietaryFocus, saturatedFatMax),
            ofWhichSugar = capTarget(DietaryFocus.SUGAR in answers.dietaryFocus, SUGAR_MAX_G),
        )
    }

    private fun rangeTarget(value: Int, spreadFraction: Double, roundTo: Int): Target {
        val min = roundToNearest((value * (1 - spreadFraction)).roundToInt(), roundTo).coerceAtLeast(0)
        val max = roundToNearest((value * (1 + spreadFraction)).roundToInt(), roundTo)
        return Target(enabled = true, min = min, max = max)
    }

    private fun capTarget(enabled: Boolean, maxValue: Int): Target =
        if (enabled) Target(enabled = true, min = 0, max = maxValue) else Target(enabled = false)

    private fun floorTarget(enabled: Boolean, minValue: Int, maxValue: Int): Target =
        if (enabled) Target(enabled = true, min = minValue, max = maxValue) else Target(enabled = false)

    private fun roundToNearest(value: Int, nearest: Int): Int = ((value + nearest / 2) / nearest) * nearest
}
