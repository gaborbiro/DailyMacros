package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.common.views.NutrientDisplayLine
import dev.gaborbiro.dailymacros.features.common.NutrientsUiMapper
import dev.gaborbiro.dailymacros.features.common.model.NutrientBreakdown
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record

internal class ModalUiMapper(
    private val nutrientsUiMapper: NutrientsUiMapper,
) {

    fun mapNutrientBreakdowns(
        record: Record,
    ): NutrientBreakdownUiModel {
        val nutrientBreakdown = record.template.nutrients
        val topContributors = record.template.topContributors

        val notes = record.template.notes

        return NutrientBreakdownUiModel(
            calories = nutrientBreakdown.calories?.let {
                nutrientsUiMapper.formatCalories(value = it, withLabel = true)
            },
            protein = nutrientBreakdown.protein?.let {
                nutrientsUiMapper.formatProtein(value = it, withLabel = true) +
                        nutrientsUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.Protein,
                            contributorText = topContributors.topProteinContributors,
                        )
            },
            fat = nutrientBreakdown.fat?.let {
                nutrientsUiMapper.formatFat(value = it, saturated = null, withLabel = true) +
                        nutrientsUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.Fat,
                            contributorText = topContributors.topFatContributors,
                        )
            },
            ofWhichSaturated = nutrientBreakdown.ofWhichSaturated?.let {
                nutrientsUiMapper.formatSaturatedFat(value = it, withLabel = true) +
                        nutrientsUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.OfWhichSaturated,
                            contributorText = topContributors.topSaturatedFatContributors,
                        )
            },
            carbs = nutrientBreakdown.carbs?.let {
                nutrientsUiMapper.formatCarbs(value = it, sugar = null, addedSugar = null, withLabel = true) +
                        nutrientsUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.Carb,
                            contributorText = topContributors.topCarbsContributors,
                        )
            },
            ofWhichSugar = nutrientBreakdown.ofWhichSugar?.let {
                nutrientsUiMapper.formatSugar(value = it, withLabel = true) +
                        nutrientsUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.OfWhichSugar,
                            contributorText = topContributors.topSugarContributors,
                        )
            },
            ofWhichAddedSugar = nutrientBreakdown.ofWhichAddedSugar?.let {
                nutrientsUiMapper.formatAddedSugar(value = it, withLabel = true) +
                        nutrientsUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.OfWhichAddedSugar,
                            contributorText = topContributors.topAddedSugarContributors,
                        )
            },
            salt = nutrientBreakdown.salt?.let {
                nutrientsUiMapper.formatSalt(value = it, withLabel = true) +
                        nutrientsUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.Salt,
                            contributorText = topContributors.topSaltContributors,
                        )
            },
            fibre = nutrientBreakdown.fibre?.let {
                nutrientsUiMapper.formatFibre(value = it, withLabel = true) +
                        nutrientsUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.Fibre,
                            contributorText = topContributors.topFibreContributors,
                        )
            },
            notes = notes,
        )
    }

    fun mapMacrosPrintout(nutrientBreakdown: NutrientBreakdown?): String? {
        return listOfNotNull(
            nutrientBreakdown?.calories?.let { nutrientsUiMapper.formatCalories(it, withLabel = true) },
            nutrientBreakdown?.protein?.let { nutrientsUiMapper.formatProtein(it, withLabel = true) },
            nutrientBreakdown?.fat?.let { nutrientsUiMapper.formatFat(it, nutrientBreakdown.ofWhichSaturated, withLabel = true) },
            nutrientBreakdown?.carbs?.let { nutrientsUiMapper.formatCarbs(it, nutrientBreakdown.ofWhichSugar, nutrientBreakdown.ofWhichAddedSugar, withLabel = true) },
            nutrientBreakdown?.salt?.let { nutrientsUiMapper.formatSalt(it, withLabel = true) },
            nutrientBreakdown?.fibre?.let { nutrientsUiMapper.formatFibre(it, withLabel = true) }
        )
            .joinToString()
            .takeIf { it.isNotBlank() }
    }
}
