package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.features.common.model.NutrientBreakdown
import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientsApiModel
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors

internal class RecordsMapper(
    private val nutrientsUiMapper: NutrientsUiMapper,
) {

    fun mapToFoodRecognitionRequest(base64Images: List<String>): FoodRecognitionRequest {
        return FoodRecognitionRequest(
            base64Images = base64Images,
        )
    }

    fun mapRecognisedFood(response: FoodRecognitionResult): RecognisedFood {
        return RecognisedFood(
            title = response.title,
            description = response.description,
        )
    }

    fun mapToNutrientAnalysisRequest(record: Record, base64Images: List<String>): NutrientAnalysisRequest {
        return NutrientAnalysisRequest(
            base64Images = base64Images,
            title = record.template.name,
            description = record.template.description,
        )
    }

    fun mapNutrientAnalysisResponse(response: NutrientAnalysisResult): Pair<Pair<NutrientBreakdown, TopContributors>?, String?> {
        val nutrients = response.nutrients?.let {
            mapBreakdown(it) to mapContributors(it)
        }

        return nutrients to response.error
    }

    private fun mapBreakdown(nutrientsApiModel: NutrientsApiModel): NutrientBreakdown {
        return NutrientBreakdown(
            calories = nutrientsApiModel.calories,
            protein = nutrientsApiModel.protein?.grams,
            fat = nutrientsApiModel.fat?.grams,
            ofWhichSaturated = nutrientsApiModel.ofWhichSaturated?.grams,
            carbs = nutrientsApiModel.carb?.grams,
            ofWhichSugar = nutrientsApiModel.ofWhichSugar?.grams,
            ofWhichAddedSugar = nutrientsApiModel.ofWhichAddedSugar?.grams,
            salt = nutrientsApiModel.salt?.grams,
            fibre = nutrientsApiModel.fibre?.grams,
        )
    }

    private fun mapContributors(nutrientsApiModel: NutrientsApiModel): TopContributors {
        return TopContributors(
            topProteinContributors = nutrientsApiModel.protein?.topContributors,
            topFatContributors = nutrientsApiModel.fat?.topContributors,
            topSaturatedFatContributors = nutrientsApiModel.ofWhichSaturated?.topContributors,
            topCarbsContributors = nutrientsApiModel.carb?.topContributors,
            topSugarContributors = nutrientsApiModel.ofWhichSugar?.topContributors,
            topAddedSugarContributors = nutrientsApiModel.ofWhichAddedSugar?.topContributors,
            topSaltContributors = nutrientsApiModel.salt?.topContributors,
            topFibreContributors = nutrientsApiModel.fibre?.topContributors,
        )
    }

    fun mapMacrosPrintout(nutrientBreakdown: NutrientBreakdown?, isShort: Boolean = false): String? {
        return listOfNotNull(
            nutrientBreakdown?.calories?.let { nutrientsUiMapper.formatCalories(it, isShort, withLabel = true) },
            nutrientBreakdown?.protein?.let { nutrientsUiMapper.formatProtein(it, isShort, withLabel = true) },
            nutrientBreakdown?.fat?.let { nutrientsUiMapper.formatFat(it, nutrientBreakdown.ofWhichSaturated, isShort, withLabel = true) },
            if (!isShort) {
                nutrientBreakdown?.ofWhichSaturated?.let { nutrientsUiMapper.formatSaturatedFat(it, isShort = false, withLabel = true) }
            } else null,
            nutrientBreakdown?.carbs?.let { nutrientsUiMapper.formatCarbs(it, nutrientBreakdown.ofWhichSugar, nutrientBreakdown.ofWhichAddedSugar, isShort, withLabel = true) },
            if (!isShort) {
                nutrientBreakdown?.ofWhichSugar?.let { nutrientsUiMapper.formatSugar(it, isShort = false, withLabel = true) }
            } else null,
            if (!isShort) {
                nutrientBreakdown?.ofWhichAddedSugar?.let { nutrientsUiMapper.formatAddedSugar(it, isShort = false, withLabel = true) }
            } else null,
            nutrientBreakdown?.salt?.let { nutrientsUiMapper.formatSalt(it, isShort, withLabel = true) },
            nutrientBreakdown?.fibre?.let { nutrientsUiMapper.formatFibre(it, isShort, withLabel = true) }
        )
            .joinToString()
            .takeIf { it.isNotBlank() }
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