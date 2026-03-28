package dev.gaborbiro.dailymacros.features.modal

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
                    gramDecimalPlaces = 0,
                    contributorText = topContributors.topProteinContributors,
                )
            },
            fat = fat?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.fat,
                    gramDecimalPlaces = 0,
                    contributorText = topContributors.topFatContributors,
                )
            },
            ofWhichSaturated = ofWhichSaturated?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.ofWhichSaturated,
                    gramDecimalPlaces = 0,
                    contributorText = topContributors.topSaturatedFatContributors,
                )
            },
            carbs = carbs?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.carbs,
                    gramDecimalPlaces = 0,
                    contributorText = topContributors.topCarbsContributors,
                )
            },
            ofWhichSugar = ofWhichSugar?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.ofWhichSugar,
                    gramDecimalPlaces = 0,
                    contributorText = topContributors.topSugarContributors,
                )
            },
            ofWhichAddedSugar = ofWhichAddedSugar?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.ofWhichAddedSugar,
                    gramDecimalPlaces = 0,
                    contributorText = topContributors.topAddedSugarContributors,
                )
            },
            salt = salt?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.salt,
                    gramDecimalPlaces = 2,
                    contributorText = topContributors.topSaltContributors,
                )
            },
            fibre = fibre?.let {
                it + nutrientsUiMapper.formatTopContributorSuffix(
                    amount = nutrientBreakdown.fibre,
                    gramDecimalPlaces = 0,
                    contributorText = topContributors.topFibreContributors,
                )
            },
            notes = notes,
        )
    }
}
