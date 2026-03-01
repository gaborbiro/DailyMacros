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
        val topProteinContributors = nutrientsUiMapper.formatTopContributorText(topContributors.topProteinContributors)
        val fat = nutrientsUiMapper.formatFat(value = nutrientBreakdown.fat, saturated = null, isShort = false, withLabel = true)
        val topFatContributors = nutrientsUiMapper.formatTopContributorText(topContributors.topFatContributors)
        val ofWhichSaturated = nutrientsUiMapper.formatSaturatedFat(value = nutrientBreakdown.ofWhichSaturated, isShort = false, withLabel = true)
        val topSaturatedFatContributors = nutrientsUiMapper.formatTopContributorText(topContributors.topSaturatedFatContributors)
        val carbs = nutrientsUiMapper.formatCarbs(value = nutrientBreakdown.carbs, sugar = null, addedSugar = null, isShort = false, withLabel = true)
        val topCarbsContributors = nutrientsUiMapper.formatTopContributorText(topContributors.topCarbsContributors)
        val ofWhichSugar = nutrientsUiMapper.formatSugar(value = nutrientBreakdown.ofWhichSugar, isShort = false, withLabel = true)
        val topSugarContributors = nutrientsUiMapper.formatTopContributorText(topContributors.topSugarContributors)
        val ofWhichAddedSugar = nutrientsUiMapper.formatAddedSugar(value = nutrientBreakdown.ofWhichAddedSugar, isShort = false, withLabel = true)
        val topAddedSugarContributors = nutrientsUiMapper.formatTopContributorText(topContributors.topAddedSugarContributors)
        val salt = nutrientsUiMapper.formatSalt(value = nutrientBreakdown.salt, isShort = false, withLabel = true)
        val topSaltContributors = nutrientsUiMapper.formatTopContributorText(topContributors.topSaltContributors)
        val fibre = nutrientsUiMapper.formatFibre(value = nutrientBreakdown.fibre, isShort = false, withLabel = true)
        val topFibreContributors = nutrientsUiMapper.formatTopContributorText(topContributors.topFibreContributors)
        val notes = record.template.notes

        return NutrientBreakdownUiModel(
            calories = calories,
            protein = protein?.let { it + topProteinContributors },
            fat = fat?.let { it + topFatContributors },
            ofWhichSaturated = ofWhichSaturated?.let { it + topSaturatedFatContributors },
            carbs = carbs?.let { it + topCarbsContributors },
            ofWhichSugar = ofWhichSugar?.let { it + topSugarContributors },
            ofWhichAddedSugar = ofWhichAddedSugar?.let { it + topAddedSugarContributors },
            salt = salt?.let { it + topSaltContributors },
            fibre = fibre?.let { it + topFibreContributors },
            notes = notes,
        )
    }
}
