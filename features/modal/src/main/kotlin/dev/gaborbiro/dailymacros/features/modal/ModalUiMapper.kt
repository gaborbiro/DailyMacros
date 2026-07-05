package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.common.views.NutrientDisplayLine
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.features.shared.TemplateUiMapper
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.repositories.records.domain.model.ComponentConfidence
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import javax.inject.Inject

class ModalUiMapper @Inject constructor(
    private val templateUiMapper: TemplateUiMapper,
) {
    fun mapCompactNutrients(record: Record): NutrientsUiModel =
        templateUiMapper.mapRecordNutrients(record.template.nutrients)

    fun mapNutrientBreakdowns(
        record: Record,
    ): NutrientBreakdownUiModel {
        val nutrientBreakdown = record.template.nutrients
        val topContributors = record.template.topContributors

        val notes = record.template.notes

        return NutrientBreakdownUiModel(
            calories = nutrientBreakdown.calories?.let {
                templateUiMapper.formatCalories(value = it, withLabel = true)
            },
            protein = nutrientBreakdown.protein?.let {
                templateUiMapper.formatProtein(value = it, withLabel = true) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.Protein,
                            contributorText = topContributors.topProteinContributors,
                        )
            },
            fat = nutrientBreakdown.fat?.let {
                templateUiMapper.formatFat(value = it, saturated = null, withLabel = true) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.Fat,
                            contributorText = topContributors.topFatContributors,
                        )
            },
            ofWhichSaturated = nutrientBreakdown.ofWhichSaturated?.let {
                templateUiMapper.formatSaturatedFat(value = it, withLabel = true) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.OfWhichSaturated,
                            contributorText = topContributors.topSaturatedFatContributors,
                        )
            },
            carbs = nutrientBreakdown.carbs?.let {
                templateUiMapper.formatCarbs(value = it, sugar = null, addedSugar = null, withLabel = true) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.Carb,
                            contributorText = topContributors.topCarbsContributors,
                        )
            },
            ofWhichSugar = nutrientBreakdown.ofWhichSugar?.let {
                templateUiMapper.formatSugar(value = it, withLabel = true) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.OfWhichSugar,
                            contributorText = topContributors.topSugarContributors,
                        )
            },
            ofWhichAddedSugar = nutrientBreakdown.ofWhichAddedSugar?.let {
                templateUiMapper.formatAddedSugar(value = it, withLabel = true) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.OfWhichAddedSugar,
                            contributorText = topContributors.topAddedSugarContributors,
                        )
            },
            salt = nutrientBreakdown.salt?.let {
                templateUiMapper.formatSalt(value = it, withLabel = true) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.Salt,
                            contributorText = topContributors.topSaltContributors,
                        )
            },
            fibre = nutrientBreakdown.fibre?.let {
                templateUiMapper.formatFibre(value = it, withLabel = true) +
                        templateUiMapper.formatTopContributorSuffix(
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
                dialog.imageFilenames != p.imageFilenames
    }

    fun imagesRequireMacroReanalysis(pristine: List<String>, current: List<String>): Boolean =
        pristine.toSet() != current.toSet()
}
