package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodRecognitionResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientAnalysisResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientsApiModel
import dev.gaborbiro.dailymacros.repo.records.domain.model.NutrientsBreakdown
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

    fun mapNutrientAnalysisResponse(response: NutrientAnalysisResponse): Pair<NutrientsBreakdown?, String?> {
        return Pair(
            first = response.nutrients?.let { map(it, response.description) },
            second = response.issues,
        )
    }

    private fun map(nutrientsApiModel: NutrientsApiModel, notes: String?): NutrientsBreakdown {
        return NutrientsBreakdown(
            calories = nutrientsApiModel.calories,
            protein = nutrientsApiModel.proteinGrams,
            fat = nutrientsApiModel.fatGrams,
            ofWhichSaturated = nutrientsApiModel.ofWhichSaturatedGrams,
            carbs = nutrientsApiModel.carbGrams,
            ofWhichSugar = nutrientsApiModel.ofWhichSugarGrams,
            ofWhichAddedSugar = nutrientsApiModel.ofWhichAddedSugarGrams,
            salt = nutrientsApiModel.saltGrams,
            fibre = nutrientsApiModel.fibreGrams,
            notes = notes,
        )
    }
}
