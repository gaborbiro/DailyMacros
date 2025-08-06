package dev.gaborbiro.nutri.features.common

import dev.gaborbiro.nutri.data.chatgpt.model.FoodPicSummaryRequest
import dev.gaborbiro.nutri.data.chatgpt.model.FoodPicSummaryResponse
import dev.gaborbiro.nutri.data.chatgpt.model.NutrientApiModel
import dev.gaborbiro.nutri.data.chatgpt.model.NutrientsRequest
import dev.gaborbiro.nutri.data.chatgpt.model.NutrientsResponse
import dev.gaborbiro.nutri.data.records.domain.model.Nutrients
import dev.gaborbiro.nutri.data.records.domain.model.Record

class RecordsMapper {

    fun mapFoodPicsSummaryRequest(base64Image: String): FoodPicSummaryRequest {
        return FoodPicSummaryRequest(
            base64Image = base64Image,
        )
    }

    fun map(response: FoodPicSummaryResponse): String? {
        return response.title
    }

    fun mapNutrientsRequest(record: Record, base64Image: String? = null): NutrientsRequest {
        return NutrientsRequest(
            base64Image = base64Image,
            title = record.template.name,
            description = record.template.description,
        )
    }

    fun map(response: NutrientsResponse): Pair<Nutrients?, String> {
        return response.nutrients?.let(::map) to response.comments
    }

    private fun map(nutrientApiModel: NutrientApiModel): Nutrients {
        return Nutrients(
            calories = nutrientApiModel.kcal,
            protein = nutrientApiModel.proteinGrams,
            carbohydrates = nutrientApiModel.carbGrams,
            fat = nutrientApiModel.fatGrams,
        )
    }
}
