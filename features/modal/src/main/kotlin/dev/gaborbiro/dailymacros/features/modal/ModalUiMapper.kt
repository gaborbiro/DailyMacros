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
        val componentLabels = record.template.mealComponents.map { component ->
            val confidence = when (component.confidence) {
                ComponentConfidence.MEDIUM -> " (?)"
                ComponentConfidence.LOW -> " (??)"
                else -> ""
            }
            "${component.estimatedAmount} ${component.name}$confidence"
        }

        return NutrientBreakdownUiModel(
            calories = nutrientBreakdown.calories?.let {
                "Calories: " + templateUiMapper.formatCalories(value = it)
            },
            protein = nutrientBreakdown.protein?.let {
                "Protein: " + templateUiMapper.formatProtein(value = it) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.Protein,
                            contributorText = topContributors.topProteinContributors,
                        )
            },
            fat = nutrientBreakdown.fat?.let {
                "Fat: " + templateUiMapper.formatFat(value = it, saturated = null) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.Fat,
                            contributorText = topContributors.topFatContributors,
                        )
            },
            ofWhichSaturated = nutrientBreakdown.ofWhichSaturated?.let {
                "of which saturated: " + templateUiMapper.formatSaturatedFat(value = it) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.OfWhichSaturated,
                            contributorText = topContributors.topSaturatedFatContributors,
                        )
            },
            carbs = nutrientBreakdown.carbs?.let {
                "Carb: " + templateUiMapper.formatCarbs(value = it, sugar = null, addedSugar = null) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.Carb,
                            contributorText = topContributors.topCarbsContributors,
                        )
            },
            ofWhichSugar = nutrientBreakdown.ofWhichSugar?.let {
                "of which sugar: " + templateUiMapper.formatSugar(value = it) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.OfWhichSugar,
                            contributorText = topContributors.topSugarContributors,
                        )
            },
            ofWhichAddedSugar = nutrientBreakdown.ofWhichAddedSugar?.let {
                "of which added sugar: " + templateUiMapper.formatAddedSugar(value = it) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.OfWhichAddedSugar,
                            contributorText = topContributors.topAddedSugarContributors,
                        )
            },
            salt = nutrientBreakdown.salt?.let {
                "Salt: " + templateUiMapper.formatSalt(value = it) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.Salt,
                            contributorText = topContributors.topSaltContributors,
                        )
            },
            fibre = nutrientBreakdown.fibre?.let {
                "Fibre: " + templateUiMapper.formatFibre(value = it) +
                        templateUiMapper.formatTopContributorSuffix(
                            amount = it,
                            line = NutrientDisplayLine.Fibre,
                            contributorText = topContributors.topFibreContributors,
                        )
            },
            notes = notes,
            components = componentLabels.map { "- $it" },
            componentsSummary = componentLabels.takeIf { it.isNotEmpty() }?.joinToString(separator = " · "),
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

    /** Mirrors when saving edits schedules a new nutrient analysis: new wording or a different set of photos. */
    fun editsRequireMacroAnalysis(dialog: DialogHandle.RecordDetailsDialog.View): Boolean {
        val pristine = dialog.pristineSnapshot
        return dialog.title.text.trim() != pristine.title.trim() ||
                dialog.description.text.trim() != pristine.description.trim() ||
                imagesRequireMacroReanalysis(pristine.imageFilenames, dialog.imageFilenames)
    }

    /** The description the AI re-analyzes with: the existing one, followed by the user's correction on a new line. */
    fun appendCorrection(description: String, correction: String): String =
        listOf(description.trim(), correction.trim())
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")

    /** How long a correction can be before the combined description exceeds [maxDescriptionLength]. */
    fun correctionCapacity(description: String, maxDescriptionLength: Int): Int {
        val existing = description.trim()
        val separatorLength = if (existing.isEmpty()) 0 else 1
        return (maxDescriptionLength - existing.length - separatorLength).coerceAtLeast(0)
    }
}
