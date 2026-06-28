package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

data class NutrientAnalysis(
    val nutrients: Nutrients?,
    val title: String?,
    val notes: String?,
    val components: List<MealComponent>,
    /** Per submitted photo index; null if model omitted `representative_of_meal` or that index. */
    val isRepresentativeOfMealByImageIndex: List<Boolean?>,
    val error: String?,
)

data class Nutrients(
    val calories: Int?,
    val protein: Nutrient?,
    val fat: Nutrient?,
    val ofWhichSaturated: Nutrient?,
    val carb: Nutrient?,
    val ofWhichSugar: Nutrient?,
    val ofWhichAddedSugar: Nutrient?,
    val salt: Nutrient?,
    val fibre: Nutrient?,
)

data class Nutrient(
    val grams: Float?,
    val topContributors: String?,
)

/**
 * Parsed from ChatGPT nutrient / recognition JSON before persistence.
 */
data class MealComponent(
    val name: String,
    val estimatedAmount: String,
    val confidence: String,
)
