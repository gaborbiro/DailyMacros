package dev.gaborbiro.dailymacros.features.common.model

import dev.gaborbiro.dailymacros.repo.records.domain.model.TemplateNutrientBreakdown

/**
 * null doesn't mean 0 for that nutrient. It means it's unknown.
 */
data class NutrientBreakdown(
    val calories: Int? = null,
    val protein: Float? = null,
    val fat: Float? = null,
    val ofWhichSaturated: Float? = null,
    val carbs: Float? = null,
    val ofWhichSugar: Float? = null,
    val ofWhichAddedSugar: Float? = null,
    val salt: Float? = null,
    val fibre: Float? = null,
) {
    companion object {
        fun fromTemplate(template: TemplateNutrientBreakdown): NutrientBreakdown {
            return NutrientBreakdown(
                calories = template.calories,
                protein = template.protein,
                fat = template.fat,
                ofWhichSaturated = template.ofWhichSaturated,
                carbs = template.carbs,
                ofWhichSugar = template.ofWhichSugar,
                ofWhichAddedSugar = template.ofWhichAddedSugar,
                salt = template.salt,
                fibre = template.fibre,
            )
        }
    }
}