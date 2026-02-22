package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.modal.model.PhotoAnalysisResults
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.PhotoAnalysisRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.PhotoAnalysisResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosApiModel
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosResponse
import dev.gaborbiro.dailymacros.repo.records.domain.model.Macros
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record

internal class RecordsMapper(
    private val appPrefs: AppPrefs,
) {

    fun mapPhotoAnalysisRequest(base64Images: List<String>): PhotoAnalysisRequest {
        return PhotoAnalysisRequest(
            base64Images = base64Images,
        )
    }

    fun map(response: PhotoAnalysisResponse): PhotoAnalysisResults {
        return PhotoAnalysisResults(
            title = response.title,
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

    fun map(response: MacrosResponse): Triple<Macros?, String?, String?> {
        return Triple(
            first = response.macros?.let { map(it, response.notes) },
            second = response.issues,
            third = response.title,
        )
    }

    private fun map(macrosApiModel: MacrosApiModel, notes: String?): Macros {
        return Macros(
            calories = macrosApiModel.calories,
            protein = macrosApiModel.proteinGrams,
            fat = macrosApiModel.fatGrams,
            ofWhichSaturated = macrosApiModel.ofWhichSaturatedGrams,
            carbs = macrosApiModel.carbGrams,
            ofWhichSugar = macrosApiModel.ofWhichSugarGrams,
            ofWhichAddedSugar = macrosApiModel.ofWhichAddedSugarGrams,
            salt = macrosApiModel.saltGrams,
            fibre = macrosApiModel.fibreGrams,
            notes = notes,
        )
    }
}
