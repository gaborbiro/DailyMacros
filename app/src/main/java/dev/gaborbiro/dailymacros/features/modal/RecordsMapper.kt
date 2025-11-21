package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodPicSummaryRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodPicSummaryResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosApiModel
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosResponse
import dev.gaborbiro.dailymacros.repo.records.domain.model.Macros
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record

internal class RecordsMapper(
    private val appPrefs: AppPrefs,
) {

    fun mapFoodPicsSummaryRequest(base64Images: List<String>): FoodPicSummaryRequest {
        return FoodPicSummaryRequest(
            base64Images = base64Images,
        )
    }

    fun map(response: FoodPicSummaryResponse): DialogState.InputDialog.CreateWithImageDialog.SummarySuggestions {
        return DialogState.InputDialog.CreateWithImageDialog.SummarySuggestions(
            titles = response.titles.distinct(),
            description = response.description
        )
    }

    fun mapMacrosRequest(record: Record, base64Images: List<String>): MacrosRequest {
        return MacrosRequest(
            base64Images = base64Images,
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
            fat = macrosApiModel.fatGrams,
            ofWhichSaturated = macrosApiModel.ofWhichSaturatedGrams,
            carbs = macrosApiModel.carbGrams,
            ofWhichSugar = macrosApiModel.ofWhichSugarGrams,
            salt = macrosApiModel.saltGrams,
            fibre = macrosApiModel.fibreGrams,
            notes = notes,
        )
    }
}
