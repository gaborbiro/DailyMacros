package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.common.views.NutrientDisplayLine
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.features.modal.model.NutrientKind
import dev.gaborbiro.dailymacros.features.modal.model.NutrientRowUiModel
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
            // Only the uncertain ones are marked, in plain words: confident components need no label.
            val confidence = when (component.confidence) {
                ComponentConfidence.MEDIUM -> " (likely)"
                ComponentConfidence.LOW -> " (guess)"
                ComponentConfidence.HIGH -> ""
            }
            "${component.estimatedAmount} ${component.name}$confidence"
        }

        fun row(
            kind: NutrientKind,
            label: String,
            amount: Number?,
            line: NutrientDisplayLine,
            contributors: String?,
            indent: Int = 0,
            format: (Number) -> String,
        ): NutrientRowUiModel? = amount?.let {
            NutrientRowUiModel(
                kind = kind,
                label = label,
                amount = format(it),
                sources = templateUiMapper.topContributors(amount = it, line = line, contributorText = contributors),
                indent = indent,
            )
        }

        val rows = listOfNotNull(
            row(NutrientKind.CALORIES, "Calories", nutrientBreakdown.calories, NutrientDisplayLine.Calories, null) {
                templateUiMapper.formatCalories(it)
            },
            row(NutrientKind.PROTEIN, "Protein", nutrientBreakdown.protein, NutrientDisplayLine.Protein, topContributors.topProteinContributors) {
                templateUiMapper.formatProtein(it)
            },
            row(NutrientKind.FAT, "Fat", nutrientBreakdown.fat, NutrientDisplayLine.Fat, topContributors.topFatContributors) {
                templateUiMapper.formatFat(value = it, saturated = null)
            },
            row(
                NutrientKind.FAT, "Saturated", nutrientBreakdown.ofWhichSaturated, NutrientDisplayLine.OfWhichSaturated,
                topContributors.topSaturatedFatContributors, indent = 1,
            ) { templateUiMapper.formatSaturatedFat(it) },
            row(NutrientKind.CARBS, "Carbs", nutrientBreakdown.carbs, NutrientDisplayLine.Carb, topContributors.topCarbsContributors) {
                templateUiMapper.formatCarbs(value = it, sugar = null, addedSugar = null)
            },
            row(
                NutrientKind.CARBS, "Sugar", nutrientBreakdown.ofWhichSugar, NutrientDisplayLine.OfWhichSugar,
                topContributors.topSugarContributors, indent = 1,
            ) { templateUiMapper.formatSugar(it) },
            row(
                NutrientKind.CARBS, "Added sugar", nutrientBreakdown.ofWhichAddedSugar, NutrientDisplayLine.OfWhichAddedSugar,
                topContributors.topAddedSugarContributors, indent = 2,
            ) { templateUiMapper.formatAddedSugar(it) },
            row(NutrientKind.SALT, "Salt", nutrientBreakdown.salt, NutrientDisplayLine.Salt, topContributors.topSaltContributors) {
                templateUiMapper.formatSalt(it)
            },
            row(NutrientKind.FIBRE, "Fibre", nutrientBreakdown.fibre, NutrientDisplayLine.Fibre, topContributors.topFibreContributors) {
                templateUiMapper.formatFibre(it)
            },
        )

        val componentsSummary = componentLabels.takeIf { it.isNotEmpty() }?.joinToString(separator = " · ")
        val hasNotes = notes.isNotBlank()

        return NutrientBreakdownUiModel(
            nutrients = rows,
            notes = notes,
            components = componentLabels.map { "- $it" },
            componentsSummary = componentsSummary,
            hasAiInference = hasNotes || componentLabels.isNotEmpty(),
            aiInferencePreview = componentsSummary ?: notes,
            // Notes aren't part of the preview while it shows the components, so they'd be unreachable otherwise.
            notesHiddenByPreview = componentsSummary != null && hasNotes,
        ).let { model -> model.copy(hasDisplayableContent = hasDisplayableContent(model)) }
    }

    fun hasDisplayableContent(model: NutrientBreakdownUiModel): Boolean =
        model.nutrients.any { it.amount.isNotBlank() } || !model.notes.isNullOrBlank() || model.components.isNotEmpty()

    fun hasUnsavedEdits(dialog: DialogHandle.RecordDetailsDialog): Boolean {
        val pristine = when (dialog) {
            is DialogHandle.RecordDetailsDialog.View -> dialog.pristineSnapshot
            is DialogHandle.RecordDetailsDialog.Edit -> dialog.pristineSnapshot
        }
        return dialog.title.text != pristine.title ||
                dialog.description.text != pristine.description ||
                dialog.imageFilenames != pristine.imageFilenames
    }
}
