package dev.gaborbiro.dailymacros.features.common

import android.icu.text.DecimalFormat
import android.util.Range
import dev.gaborbiro.dailymacros.features.common.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.features.common.views.NutrientDisplayLine
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import kotlin.math.pow

internal class NutrientsUiMapper {

    fun mapRecordNutrients(nutrientBreakdown: TemplateNutrientBreakdown): NutrientsUiModel {
        return NutrientsUiModel(
            calories = nutrientBreakdown.calories?.let {
                formatCalories(nutrientBreakdown.calories, withLabel = false)
            },
            protein = nutrientBreakdown.protein?.let {
                formatProtein(nutrientBreakdown.protein, withLabel = true)
            },
            fat = nutrientBreakdown.fat?.let {
                formatFat(nutrientBreakdown.fat, nutrientBreakdown.ofWhichSaturated, withLabel = true)
            },
            carbs = nutrientBreakdown.carbs?.let {
                formatCarbs(nutrientBreakdown.carbs, nutrientBreakdown.ofWhichSugar, nutrientBreakdown.ofWhichAddedSugar, withLabel = true)
            },
            salt = nutrientBreakdown.salt?.let {
                formatSalt(nutrientBreakdown.salt, withLabel = true)
            },
            fibre = nutrientBreakdown.fibre?.let {
                formatFibre(nutrientBreakdown.fibre, withLabel = true)
            },
        )
    }

    fun formatCalories(
        value: Number?,
        withLabel: Boolean,
    ): String {
        val amount = value ?: 0
        val label = if (withLabel) "Calories:" else null
        val format = generateFormat(
            label = label,
            line = NutrientDisplayLine.Calories,
        )
        return formatMacroAmount(amount, format)
    }

    fun formatProtein(
        value: Number?,
        withLabel: Boolean,
    ): String {
        val amount = value ?: 0
        val label = if (withLabel) "Protein:" else null
        val format = generateFormat(
            label = label,
            line = NutrientDisplayLine.Protein,
        )
        return formatMacroAmount(amount, format)
    }

    fun formatCarbs(
        value: Number?,
        sugar: Number?,
        addedSugar: Number?,
        withLabel: Boolean,
    ): String {
        val amount = value ?: 0
        val label = if (withLabel) "Carb:" else null
        val format = generateFormat(
            label = label,
            line = NutrientDisplayLine.Carb,
        )
        return formatMacroAmount(amount, format) {
            sugar?.let {
                val sugar = formatSugar(sugar, withLabel = false)
                val addedSugar = addedSugar?.let {
                    formatAddedSugar(addedSugar, withLabel = false)
                }
                "${sugar}${addedSugar?.let { "/$it" } ?: ""}"
            }
        }
    }

    fun formatSugar(
        value: Number?,
        withLabel: Boolean,
    ): String {
        val amount = value ?: 0
        // no short label because in short mode sugar is displayed after carbs in parentheses
        val label = if (withLabel) "of which sugar:" else null
        val format = generateFormat(
            label = label,
            line = NutrientDisplayLine.OfWhichSugar,
        )
        return formatMacroAmount(amount, format)
    }

    fun formatAddedSugar(
        value: Number?,
        withLabel: Boolean,
    ): String {
        val amount = value ?: 0
        val label = if (withLabel) "of which added sugar:" else null
        // no short label because in short mode sugar is displayed after carbs in parentheses
        val format = generateFormat(
            label = label,
            line = NutrientDisplayLine.OfWhichAddedSugar,
        )
        return formatMacroAmount(amount, format)
    }

    fun formatFat(
        value: Number?,
        saturated: Number?,
        withLabel: Boolean,
    ): String {
        val amount = value ?: 0
        val label = if (withLabel) "Fat:" else null
        val format = generateFormat(
            label = label,
            line = NutrientDisplayLine.Fat,
        )
        return formatMacroAmount(amount, format) {
            saturated?.let {
                formatSaturatedFat(saturated, withLabel = false)
            }
        }
    }

    fun formatSaturatedFat(
        value: Number?,
        withLabel: Boolean,
    ): String {
        val amount = value ?: 0
        // no short label because in short mode saturated fats are displayed after fat in parentheses
        val label = if (withLabel) "of which saturated:" else null
        val format = generateFormat(
            label = label,
            line = NutrientDisplayLine.OfWhichSaturated,
        )
        return formatMacroAmount(amount, format)
    }

    fun formatSalt(
        value: Number?,
        withLabel: Boolean,
    ): String {
        val amount = value ?: 0
        val label = if (withLabel) "Salt:" else null
        val format = generateFormat(
            label = label,
            line = NutrientDisplayLine.Salt,
        )
        return formatMacroAmount(amount, format)
    }

    fun formatFibre(
        value: Number?,
        withLabel: Boolean,
    ): String {
        val amount = value ?: 0
        val label = if (withLabel) "Fibre:" else null
        val format = generateFormat(
            label = label,
            line = NutrientDisplayLine.Fibre,
        )
        return formatMacroAmount(amount, format)
    }

    fun targetProgress(target: Target, total: Float): Float? = target.max?.let { total / it }

    fun targetRange(target: Target): Range<Float> {
        val min = target.min
        val max = target.max
        return Range(
            /* lower = */ if (min != null && max != null) {
                min.toFloat() / max.toFloat()
            } else {
                0f
            },
            /* upper = */ 1f
        )
    }

    private fun formatMacroAmount(
        value: Number,
        format: SafeNumberFormatter,
        extraValue: (() -> String?)? = null,
    ): String {
        return format.format(value) + (extraValue?.invoke()?.let { "($it)" } ?: "")
    }

    private fun generateFormat(
        label: String?,
        line: NutrientDisplayLine,
    ): SafeNumberFormatter {
        val decimalPattern = "${decimalFormat(line.decimalCount, line.dropTrailingZeroes)}${line.unit.literal()}"

        val format = if (label != null) {
            val labelLiteral = "$label ".literal()
            DecimalFormat("$labelLiteral$decimalPattern")
        } else {
            DecimalFormat(decimalPattern)
        }

        val safeFormatter = object : SafeNumberFormatter(format) {
            override fun nullFormat(): String = label ?: ""
        }

        return safeFormatter
    }

    private fun decimalFormat(decimals: Int, dropTrailingZeroes: Boolean = true): String {
        require(decimals >= 0)
        val pattern = buildString {
            append("0")
            if (decimals > 0) {
                append('.')
                val ch = if (dropTrailingZeroes) '#' else '0'
                repeat(decimals) { append(ch) }
            }
        }
        return pattern
    }

    /**
     * @return receiver wrapped in '' if non blank, [default] otherwise
     */
    private fun String.literal(
        default: String? = "",
    ) = takeIf(String::isNotBlank)
        ?.let { "'$this'" }
        ?: default

    fun formatTopContributorText(text: String?): String {
        return text
            ?.takeIf { it.isNotBlank() }
            ?.let {
                "\n    ($it)"
            } ?: ""
    }

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

    private abstract class SafeNumberFormatter(
        private val format: DecimalFormat,
    ) {
        fun format(value: Number?): String = value?.let { format.format(it) } ?: nullFormat()

        abstract fun nullFormat(): String
    }
}
