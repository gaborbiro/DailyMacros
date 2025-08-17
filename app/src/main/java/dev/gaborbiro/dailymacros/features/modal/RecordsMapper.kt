package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.repo.chatgpt.model.FoodPicSummaryRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.model.FoodPicSummaryResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.model.NutrientApiModel
import dev.gaborbiro.dailymacros.repo.chatgpt.model.NutrientsRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.model.NutrientsResponse
import dev.gaborbiro.dailymacros.repo.records.domain.model.Nutrients
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record

class RecordsMapper {

    fun mapFoodPicsSummaryRequest(base64Image: String): FoodPicSummaryRequest {
        return FoodPicSummaryRequest(
            base64Image = base64Image,
        )
    }

    fun map(response: FoodPicSummaryResponse): DialogState.InputDialog.SummarySuggestions {
        return DialogState.InputDialog.SummarySuggestions(
            titles = response.titles,
            description = response.description
        )
    }

    fun mapNutrientsRequest(record: Record, base64Image: String? = null): NutrientsRequest {
        return NutrientsRequest(
            base64Image = base64Image,
            title = record.template.name,
            description = record.template.description,
        )
    }

    fun map(response: NutrientsResponse): Triple<Nutrients?, String?, String?> {
        return Triple(response.nutrients?.let(::map), response.issues, response.notes)
    }

    private fun map(nutrientApiModel: NutrientApiModel): Nutrients {
        return Nutrients(
            calories = nutrientApiModel.calories,
            protein = nutrientApiModel.proteinGrams,
            carbohydrates = nutrientApiModel.carbGrams,
            ofWhichSugar = nutrientApiModel.ofWhichSugarGrams,
            fat = nutrientApiModel.fatGrams,
            ofWhichSaturated = nutrientApiModel.ofWhichSaturatedGrams,
            salt = nutrientApiModel.saltGrams,
            fibre = nutrientApiModel.fibreGrams,
        )
    }
}
