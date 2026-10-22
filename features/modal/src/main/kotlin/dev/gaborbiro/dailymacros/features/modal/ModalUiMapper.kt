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

        val n = nutrientBreakdown
        val c = topContributors
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
            row(NutrientKind.CALORIES, "Calories", n.calories, NutrientDisplayLine.Calories, null) {
                templateUiMapper.formatCalories(it)
            },
            row(NutrientKind.PROTEIN, "Protein", n.protein, NutrientDisplayLine.Protein, c.topProteinContributors) {
                templateUiMapper.formatProtein(it)
            },
            row(NutrientKind.FAT, "Fat", n.fat, NutrientDisplayLine.Fat, c.topFatContributors) {
                templateUiMapper.formatFat(value = it, saturated = null)
            },
            row(
                NutrientKind.FAT, "Saturated", n.ofWhichSaturated, NutrientDisplayLine.OfWhichSaturated,
                c.topSaturatedFatContributors, indent = 1,
            ) { templateUiMapper.formatSaturatedFat(it) },
            row(NutrientKind.CARBS, "Carbs", n.carbs, NutrientDisplayLine.Carb, c.topCarbsContributors) {
                templateUiMapper.formatCarbs(value = it, sugar = null, addedSugar = null)
            },
            row(
                NutrientKind.CARBS, "Sugar", n.ofWhichSugar, NutrientDisplayLine.OfWhichSugar,
                c.topSugarContributors, indent = 1,
            ) { templateUiMapper.formatSugar(it) },
            row(
                NutrientKind.CARBS, "Added sugar", n.ofWhichAddedSugar, NutrientDisplayLine.OfWhichAddedSugar,
                c.topAddedSugarContributors, indent = 2,
            ) { templateUiMapper.formatAddedSugar(it) },
            row(NutrientKind.SALT, "Salt", n.salt, NutrientDisplayLine.Salt, c.topSaltContributors) {
                templateUiMapper.formatSalt(it)
            },
            row(NutrientKind.FIBRE, "Fibre", n.fibre, NutrientDisplayLine.Fibre, c.topFibreContributors) {
                templateUiMapper.formatFibre(it)
            },
        )

        return NutrientBreakdownUiModel(
            nutrients = rows,
            notes = notes,
            components = componentLabels.map { "- $it" },
            componentsSummary = componentLabels.takeIf { it.isNotEmpty() }?.joinToString(separator = " · "),
        ).let { model -> model.copy(hasDisplayableContent = hasDisplayableContent(model)) }
    }

    fun hasDisplayableContent(model: NutrientBreakdownUiModel): Boolean =
        model.nutrients.any { it.amount.isNotBlank() } || !model.notes.isNullOrBlank() || model.components.isNotEmpty()

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
