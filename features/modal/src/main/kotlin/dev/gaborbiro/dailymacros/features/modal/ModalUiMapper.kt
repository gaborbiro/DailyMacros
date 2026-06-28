package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.common.views.NutrientDisplayLine
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.features.shared.NutrientsUiMapper
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.repositories.records.domain.model.ComponentConfidence
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import javax.inject.Inject

class ModalUiMapper @Inject constructor(
    private val nutrientsUiMapper: NutrientsUiMapper,
) {
    fun mapCompactNutrients(record: Record): NutrientsUiModel =
        nutrientsUiMapper.mapRecordNutrients(record.template.nutrients)

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
            components = record.template.mealComponents.map { component ->
                val confidence = when (component.confidence) {
                    ComponentConfidence.MEDIUM -> " (?)"
                    ComponentConfidence.LOW -> " (??)"
                    else -> ""
                }
                "- ${component.estimatedAmount} ${component.name}$confidence"
            },
        ).let { model -> model.copy(hasDisplayableContent = hasDisplayableContent(model)) }
    }

    fun hasDisplayableContent(model: NutrientBreakdownUiModel): Boolean =
        sequenceOf(
            model.calories,
            model.protein,
            model.fat,
            model.ofWhichSaturated,
            model.carbs,
            model.ofWhichSugar,
            model.ofWhichAddedSugar,
            model.salt,
            model.fibre,
        ).any { !it.isNullOrBlank() } || !model.notes.isNullOrBlank() || model.components.isNotEmpty()

    fun hasUnsavedEdits(dialog: DialogHandle.RecordDetailsDialog): Boolean {
        val p = when (dialog) {
            is DialogHandle.RecordDetailsDialog.View -> dialog.pristineSnapshot
            is DialogHandle.RecordDetailsDialog.Edit -> dialog.pristineSnapshot
        }
        return dialog.title.text != p.title ||
                dialog.description.text != p.description ||
                dialog.images != p.images
    }

    fun imagesRequireMacroReanalysis(pristine: List<String>, current: List<String>): Boolean =
        pristine.toSet() != current.toSet()
}
