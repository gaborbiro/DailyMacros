package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.repo.chatgpt.model.FoodPicSummaryRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.model.FoodPicSummaryResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.model.MacrosApiModel
import dev.gaborbiro.dailymacros.repo.chatgpt.model.MacrosRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.model.MacrosResponse
import dev.gaborbiro.dailymacros.repo.records.domain.model.Macros
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

    fun mapMacrosRequest(record: Record, base64Image: String? = null): MacrosRequest {
        return MacrosRequest(
            base64Image = base64Image,
            title = record.template.name,
            description = record.template.description,
        )
    }

    fun map(response: MacrosResponse): Pair<Macros?, String?> {
        return response.macros?.let { map(it, response.notes) } to response.issues
    }

    private fun map(macrosApiModel: MacrosApiModel, notes: String?): Macros {
        return Macros(
            calories = macrosApiModel.calories,
            protein = macrosApiModel.proteinGrams,
            carbohydrates = macrosApiModel.carbGrams,
            ofWhichSugar = macrosApiModel.ofWhichSugarGrams,
            fat = macrosApiModel.fatGrams,
            ofWhichSaturated = macrosApiModel.ofWhichSaturatedGrams,
            salt = macrosApiModel.saltGrams,
            fibre = macrosApiModel.fibreGrams,
            notes = notes,
        )
    }
}
