package dev.gaborbiro.dailymacros.features.shared

import android.icu.text.DecimalFormat
import android.util.Range
import dev.gaborbiro.dailymacros.features.common.views.NutrientDisplayLine
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import javax.inject.Inject
import kotlin.math.pow

class TemplateUiMapper @Inject constructor() {

    fun mapRecordNutrients(nutrients: Nutrients): NutrientsUiModel {
        return NutrientsUiModel(
            calories = nutrients.calories?.let { calories ->
                formatCalories(calories)
            },
            protein = nutrients.protein?.let { protein ->
                "Protein: " + formatProtein(protein)
            },
            fat = nutrients.fat?.let { fat ->
                "Fat: " + formatFat(fat, nutrients.ofWhichSaturated)
            },
            carbs = nutrients.carbs?.let { carbs ->
                "Carb: " + formatCarbs(
                    carbs,
                    nutrients.ofWhichSugar,
                    nutrients.ofWhichAddedSugar
                )
            },
            salt = nutrients.salt?.let { salt ->
                "Salt: " + formatSalt(salt)
            },
            fibre = nutrients.fibre?.let { fibre ->
                "Fibre: " + formatFibre(fibre)
            },
        )
    }

    fun formatCalories(value: Number): String =
        generateAndApplyFormat(value, NutrientDisplayLine.Calories)

    fun formatProtein(value: Number): String =
        generateAndApplyFormat(value, NutrientDisplayLine.Protein)

    fun formatCarbs(
        value: Number,
        sugar: Number?,
        addedSugar: Number?,
    ): String {
        val sugarStr = sugar
            ?.let {
                val sugar = formatSugar(sugar)
                val addedSugar = addedSugar?.let {
                    formatAddedSugar(addedSugar)
                }
                "${sugar}${addedSugar?.let { "/$it" } ?: ""}"
            }
        return generateAndApplyFormat(
            value,
            NutrientDisplayLine.Carb
        ) + sugarStr?.let { " ($it)" }.orEmpty()
    }

    fun formatSugar(value: Number): String =
        generateAndApplyFormat(value, NutrientDisplayLine.OfWhichSugar)

    fun formatAddedSugar(value: Number): String =
        generateAndApplyFormat(value, NutrientDisplayLine.OfWhichAddedSugar)

    fun formatFat(
        value: Number,
        saturated: Number?,
    ): String {
        val saturatedFat = saturated
            ?.let {
                formatSaturatedFat(saturated)
            }
        return generateAndApplyFormat(
            value,
            NutrientDisplayLine.Fat
        ) + saturatedFat?.let { " ($it)" }.orEmpty()
    }

    fun formatSaturatedFat(value: Number): String =
        generateAndApplyFormat(value, NutrientDisplayLine.OfWhichSaturated)

    fun formatSalt(value: Number): String =
        generateAndApplyFormat(value, NutrientDisplayLine.Salt)

    fun formatFibre(value: Number): String =
        generateAndApplyFormat(value, NutrientDisplayLine.Fibre)

    private fun generateAndApplyFormat(value: Number, line: NutrientDisplayLine): String {
        val format = generateFormat(
            decimalCount = line.decimalCount,
            dropTrailingZeroes = line.dropTrailingZeroes,
        )
        return format.format(value) + line.unit
    }

    private fun generateFormat(
        decimalCount: Int,
        dropTrailingZeroes: Boolean,
    ): DecimalFormat {
        require(decimalCount >= 0)
        val decimalPattern = buildString {
            append("0")
            if (decimalCount > 0) {
                append('.')
                val ch = if (dropTrailingZeroes) '#' else '0'
                repeat(decimalCount) { append(ch) }
            }
        }
        return DecimalFormat(decimalPattern)
    }

    //////////////////////////////////////////////////////////////////////////

    /**
     * Appends contributor text only when [amount] is non-null and at least the smallest
     * display step for [line] (`1 / 10^decimalCount` in the same units as formatted).
     */
    fun formatTopContributorSuffix(
        amount: Number?,
        line: NutrientDisplayLine,
        contributorText: String?,
    ): String {
        if (amount == null || amount.toDouble() < minimumAmountForContributorVisibility(line.decimalCount)) {
            return ""
        }
        return formatTopContributorText(contributorText)
    }

    private fun minimumAmountForContributorVisibility(decimalCount: Int): Double =
        1.0 / 10.0.pow(decimalCount.toDouble())

    /**
     * public for testing
     * TODO it shouldn't be tested separately like this
     */
    fun formatTopContributorText(text: String?): String {
        return text
            ?.takeIf { it.isNotBlank() }
            ?.let {
                " ($it)"
            } ?: ""
    }

    fun targetProgress(target: Target, total: Float): Float? = target.max?.let { max ->
        val min = target.min ?: 0
        if (min in 1..<max) {
            if (total <= min) {
                (total / min) * 0.75f
            } else {
                0.75f + ((total - min) / (max - min)) * 0.25f
            }
        } else {
            total / max
        }
    }

    fun targetRange(target: Target): Range<Float> {
        val min = target.min
        val max = target.max
        return Range(
            /* lower = */ when {
                min == null || max == null -> -1f  // no marker
                min == 0 -> 0f                     // marker at leftmost position
                else -> 0.75f                      // marker at fixed 75%
            },
            /* upper = */ 1f
        )
    }

    fun getBestPhoto(template: Template): String? {
        val array = template.imageFilenames.mapIndexed { index, imageFilename ->
            imageFilename to template.isRepresentativeOfMealByImageIndex[index]
        }
        return array.firstOrNull { (_, isRepresentative) -> isRepresentative == true }?.first
            ?: template.imageFilenames.firstOrNull()
    }
}
