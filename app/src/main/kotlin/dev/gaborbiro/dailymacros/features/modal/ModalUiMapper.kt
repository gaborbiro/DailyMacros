package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.common.NutrientDisplayLine
import dev.gaborbiro.dailymacros.features.common.NutrientsUiMapper
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

        val calories = nutrientsUiMapper.formatCalories(value = nutrientBreakdown.calories, isShort = false, withLabel = true)
        val protein = nutrientsUiMapper.formatProtein(value = nutrientBreakdown.protein, isShort = false, withLabel = true)
        val fat = nutrientsUiMapper.formatFat(value = nutrientBreakdown.fat, saturated = null, isShort = false, withLabel = true)
        val ofWhichSaturated = nutrientsUiMapper.formatSaturatedFat(value = nutrientBreakdown.ofWhichSaturated, isShort = false, withLabel = true)
        val carbs = nutrientsUiMapper.formatCarbs(value = nutrientBreakdown.carbs, sugar = null, addedSugar = null, isShort = false, withLabel = true)
        val ofWhichSugar = nutrientsUiMapper.formatSugar(value = nutrientBreakdown.ofWhichSugar, isShort = false, withLabel = true)
        val ofWhichAddedSugar = nutrientsUiMapper.formatAddedSugar(value = nutrientBreakdown.ofWhichAddedSugar, isShort = false, withLabel = true)
        val salt = nutrientsUiMapper.formatSalt(value = nutrientBreakdown.salt, isShort = false, withLabel = true)
        val fibre = nutrientsUiMapper.formatFibre(value = nutrientBreakdown.fibre, isShort = false, withLabel = true)
        val notes = record.template.notes

        return NutrientBreakdownUiModel(
            calories = calories,
            protein = protein?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.protein,
                    line = NutrientDisplayLine.Protein,
                    contributorText = topContributors.topProteinContributors,
                )
            },
            fat = fat?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.fat,
                    line = NutrientDisplayLine.Fat,
                    contributorText = topContributors.topFatContributors,
                )
            },
            ofWhichSaturated = ofWhichSaturated?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.ofWhichSaturated,
                    line = NutrientDisplayLine.OfWhichSaturated,
                    contributorText = topContributors.topSaturatedFatContributors,
                )
            },
            carbs = carbs?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.carbs,
                    line = NutrientDisplayLine.Carb,
                    contributorText = topContributors.topCarbsContributors,
                )
            },
            ofWhichSugar = ofWhichSugar?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.ofWhichSugar,
                    line = NutrientDisplayLine.OfWhichSugar,
                    contributorText = topContributors.topSugarContributors,
                )
            },
            ofWhichAddedSugar = ofWhichAddedSugar?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.ofWhichAddedSugar,
                    line = NutrientDisplayLine.OfWhichAddedSugar,
                    contributorText = topContributors.topAddedSugarContributors,
                )
            },
            salt = salt?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.salt,
                    line = NutrientDisplayLine.Salt,
                    contributorText = topContributors.topSaltContributors,
                )
            },
            fibre = fibre?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.fibre,
                    line = NutrientDisplayLine.Fibre,
                    contributorText = topContributors.topFibreContributors,
                )
            },
            notes = notes,
        )
    }
}
