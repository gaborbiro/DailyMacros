package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.features.shared.model.NutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysis
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors

class RecordsMapper {

    fun mapToNutrientAnalysisRequest(record: Record, base64Images: List<String>): NutrientAnalysisRequest {
        return NutrientAnalysisRequest(
            base64Images = base64Images,
            title = record.template.name,
            description = record.template.description,
        )
    }

    fun mapNutrientAnalysisResponse(response: NutrientAnalysis): Pair<Pair<NutrientBreakdown, TopContributors>?, String?> {
        val nutrients = response.nutrients?.let {
            mapBreakdown(it) to mapContributors(it)
        }

        return nutrients to response.error
    }

    private fun mapBreakdown(nutrients: Nutrients): NutrientBreakdown {
        return NutrientBreakdown(
            calories = nutrients.calories,
            protein = nutrients.protein?.grams,
            fat = nutrients.fat?.grams,
            ofWhichSaturated = nutrients.ofWhichSaturated?.grams,
            carbs = nutrients.carb?.grams,
            ofWhichSugar = nutrients.ofWhichSugar?.grams,
            ofWhichAddedSugar = nutrients.ofWhichAddedSugar?.grams,
            salt = nutrients.salt?.grams,
            fibre = nutrients.fibre?.grams,
        )
    }

    private fun mapContributors(nutrients: Nutrients): TopContributors {
        return TopContributors(
            topProteinContributors = nutrients.protein?.topContributors,
            topFatContributors = nutrients.fat?.topContributors,
            topSaturatedFatContributors = nutrients.ofWhichSaturated?.topContributors,
            topCarbsContributors = nutrients.carb?.topContributors,
            topSugarContributors = nutrients.ofWhichSugar?.topContributors,
            topAddedSugarContributors = nutrients.ofWhichAddedSugar?.topContributors,
            topSaltContributors = nutrients.salt?.topContributors,
            topFibreContributors = nutrients.fibre?.topContributors,
        )
    }

    fun map(template: TemplateNutrientBreakdown): NutrientBreakdown {
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

    fun map(template: NutrientBreakdown): TemplateNutrientBreakdown {
        return TemplateNutrientBreakdown(
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
