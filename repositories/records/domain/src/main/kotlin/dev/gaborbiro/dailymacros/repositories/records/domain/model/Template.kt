package dev.gaborbiro.dailymacros.repositories.records.domain.model

data class Template(
    val dbId: Long,
    val images: List<String>,
    /** Same order as [images]; null = never classified (legacy row or no analysis). */
    val isRepresentativeOfMealByImageIndex: List<Boolean?>,
    val name: String,
    val description: String,
    /** When this template was created by forking from another template; null for roots or legacy rows. */
    val parentTemplateId: Long? = null,
    /** Epoch ms when the template row was created (0 = legacy / epoch start default). */
    val createdAtEpochMs: Long = 0L,
    /** Epoch ms when the template row was last updated. */
    val updatedAtEpochMs: Long = 0L,
    val isPending: Boolean,
    val nutrients: TemplateNutrientBreakdown,
    val notes: String,
    /** Parsed from persisted AI analysis; empty if none or legacy data. */
    val mealComponents: List<MealComponent>,
    val topContributors: TopContributors,
    val quickPickOverride: QuickPickOverride?,
) {
    enum class QuickPickOverride {
        INCLUDE, EXCLUDE
    }
}

/**
 * null doesn't mean 0 for that nutrient. It means it's unknown.
 */

data class TemplateNutrientBreakdown(
    val calories: Int? = null,
    val protein: Float? = null,
    val fat: Float? = null,
    val ofWhichSaturated: Float? = null,
    val carbs: Float? = null,
    val ofWhichSugar: Float? = null,
    val ofWhichAddedSugar: Float? = null,
    val salt: Float? = null,
    val fibre: Float? = null,
)
