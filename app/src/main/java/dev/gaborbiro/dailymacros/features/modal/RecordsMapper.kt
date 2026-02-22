package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodRecognitionResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientAnalysisResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientsApiModel
import dev.gaborbiro.dailymacros.repo.records.domain.model.NutrientBreakdown
import dev.gaborbiro.dailymacros.repo.records.domain.model.TopContributors
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record

internal class RecordsMapper {

    fun mapToFoodRecognitionRequest(base64Images: List<String>): FoodRecognitionRequest {
        return FoodRecognitionRequest(
            base64Images = base64Images,
        )
    }

    fun mapRecognisedFood(response: FoodRecognitionResponse): RecognisedFood {
        return RecognisedFood(
            title = response.title,
            description = response.description
        )
    }

    fun mapToNutrientAnalysisRequest(record: Record, base64Images: List<String>): NutrientAnalysisRequest {
        return NutrientAnalysisRequest(
            base64Images = base64Images,
            title = record.template.name,
            description = record.template.description,
        )
    }

    fun mapNutrientAnalysisResponse(response: NutrientAnalysisResponse): Pair<Pair<NutrientBreakdown, TopContributors>?, String?> {
        val nutrients = response.nutrients?.let {
            mapBreakdown(it, response.description) to mapContributors(it)
        }

        return nutrients to response.issues
    }

    private fun mapBreakdown(nutrientsApiModel: NutrientsApiModel, notes: String?): NutrientBreakdown {
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
            notes = notes,
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
}
