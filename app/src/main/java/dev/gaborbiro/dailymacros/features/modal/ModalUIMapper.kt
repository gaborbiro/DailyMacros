package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.common.NutrientsUIMapper
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record

internal class ModalUIMapper(
    private val nutrientsUIMapper: NutrientsUIMapper,
) {

    fun mapNutrientBreakdowns(
        record: Record,
    ): NutrientBreakdownUiModel {
        val nutrientBreakdown = record.template.nutrients
        val topContributors = record.template.topContributors
        val calories = nutrientsUIMapper.formatCalories(value = nutrientBreakdown.calories, isShort = false, withLabel = true)
        val protein = nutrientsUIMapper.formatProtein(value = nutrientBreakdown.protein, isShort = false, withLabel = true)
        val topProteinContributors = nutrientsUIMapper.formatTopContributorText(topContributors.topProteinContributors)
        val fat = nutrientsUIMapper.formatFat(value = nutrientBreakdown.fat, saturated = null, isShort = false, withLabel = true)
        val topFatContributors = nutrientsUIMapper.formatTopContributorText(topContributors.topFatContributors)
        val ofWhichSaturated = nutrientsUIMapper.formatSaturatedFat(value = nutrientBreakdown.ofWhichSaturated, isShort = false, withLabel = true)
        val topSaturatedFatContributors = nutrientsUIMapper.formatTopContributorText(topContributors.topSaturatedFatContributors)
        val carbs = nutrientsUIMapper.formatCarbs(value = nutrientBreakdown.carbs, sugar = null, addedSugar = null, isShort = false, withLabel = true)
        val topCarbsContributors = nutrientsUIMapper.formatTopContributorText(topContributors.topCarbsContributors)
        val ofWhichSugar = nutrientsUIMapper.formatSugar(value = nutrientBreakdown.ofWhichSugar, isShort = false, withLabel = true)
        val topSugarContributors = nutrientsUIMapper.formatTopContributorText(topContributors.topSugarContributors)
        val ofWhichAddedSugar = nutrientsUIMapper.formatAddedSugar(value = nutrientBreakdown.ofWhichAddedSugar, isShort = false, withLabel = true)
        val topAddedSugarContributors = nutrientsUIMapper.formatTopContributorText(topContributors.topAddedSugarContributors)
        val salt = nutrientsUIMapper.formatSalt(value = nutrientBreakdown.salt, isShort = false, withLabel = true)
        val topSaltContributors = nutrientsUIMapper.formatTopContributorText(topContributors.topSaltContributors)
        val fibre = nutrientsUIMapper.formatFibre(value = nutrientBreakdown.fibre, isShort = false, withLabel = true)
        val topFibreContributors = nutrientsUIMapper.formatTopContributorText(topContributors.topFibreContributors)
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
