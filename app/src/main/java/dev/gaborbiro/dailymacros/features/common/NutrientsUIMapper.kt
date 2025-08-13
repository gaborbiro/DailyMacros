package dev.gaborbiro.dailymacros.features.common

import android.icu.text.DecimalFormat
import dev.gaborbiro.dailymacros.data.records.domain.model.Nutrients

internal class NutrientsUIMapper {

    fun map(nutrients: Nutrients?, isShort: Boolean = false): String? {
        return listOfNotNull(
            nutrients?.calories?.let { mapCalories(it, isShort) },
            nutrients?.protein?.let { mapProtein(it, isShort) },
            nutrients?.carbohydrates?.let { mapCarbohydrates(it, nutrients.ofWhichSugar, isShort) },
            if (!isShort) {
                nutrients?.ofWhichSugar?.let { mapSugar(it, isShort = false) }
            } else null,
            nutrients?.fat?.let { mapFat(it, nutrients.ofWhichSaturated, isShort) },
            if (!isShort) {
                nutrients?.ofWhichSaturated?.let { mapSaturated(it, isShort = false) }
            } else null,
            nutrients?.salt?.let { mapSalt(it, isShort) },
            nutrients?.fibre?.let { mapFibre(it, isShort) }
        )
            .joinToString()
            .takeIf { it.isNotBlank() }
    }

    fun mapCalories(
        value: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val (shortFormat, longFormat) = generateFormats(
            label = "Calories:",
            shortLabel = "",
            unit = "cal",
            withLabel = withLabel,
        )
        return map(value, shortFormat, longFormat, isShort)
    }

    fun mapProtein(
        value: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val (shortFormat, longFormat) = generateFormats(
            label = "Protein:",
            unit = "g",
            withLabel = withLabel,
        )
        return map(value, shortFormat, longFormat, isShort)
    }

    fun mapCarbohydrates(
        value: Number?,
        sugar: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val (shortFormat, longFormat) = generateFormats(
            label = "Carbs:",
            unit = "g",
            withLabel = withLabel,
        )
        return map(value, shortFormat, longFormat, isShort) {
            mapSugar(sugar, isShort, withLabel = false)
        }
    }

    fun mapSugar(
        value: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val (shortFormat, longFormat) = generateFormats(
            label = "of which sugar:",
            shortLabel = "",
            unit = "g",
            withLabel = withLabel,
        )
        return map(value, shortFormat, longFormat, isShort)
    }

    fun mapFat(
        value: Number?,
        saturated: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val (shortFormat, longFormat) = generateFormats(
            label = "Fat:",
            unit = "g",
            withLabel = withLabel,
        )
        return map(value, shortFormat, longFormat, isShort) {
            mapSaturated(saturated, isShort, withLabel = false)
        }
    }

    fun mapSaturated(
        value: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val (shortFormat, longFormat) = generateFormats(
            label = "of which saturated:",
            shortLabel = "",
            unit = "g",
            withLabel = withLabel,
        )
        return map(value, shortFormat, longFormat, isShort)
    }

    fun mapSalt(
        value: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val (shortFormat, longFormat) = generateFormats(
            label = "Salt:",
            unit = "g",
            withLabel = withLabel,
        )
        return map(value, shortFormat, longFormat, isShort)
    }

    fun mapFibre(
        value: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val (shortFormat, longFormat) = generateFormats(
            label = "Fibre:",
            unit = "g",
            withLabel = withLabel,
        )
        return map(value, shortFormat, longFormat, isShort)
    }

    private fun map(
        value: Number?,
        shortFormat: DecimalFormat,
        longFormat: DecimalFormat,
        isShort: Boolean = false,
        contractedValue: (() -> String?)? = null,
    ): String? {
        return value
            ?.let {
                if (isShort) {
                    shortFormat.format(value) + (contractedValue?.invoke()?.let { "($it)" } ?: "")
                } else {
                    longFormat.format(value)
                }
            }
            ?: run {
                if (isShort) {
                    null
                } else {
                    longFormat.format(value ?: 0)
                }
            }
    }

    private fun generateFormats(
        label: String,
        shortLabel: String? = label,
        unit: String,
        withLabel: Boolean,
    ): Pair<DecimalFormat, DecimalFormat> {
        val unitLiteral = unit.literal()
        val decimalFormat = "0.#$unitLiteral"
        val labelLiteral = "$label ".literal()
        val shortLabelLiteral = "$shortLabel ".literal()
        val shortFormat =
            if (withLabel) DecimalFormat("$shortLabelLiteral$decimalFormat") else DecimalFormat(decimalFormat)
        val longFormat =
            if (withLabel) DecimalFormat("$labelLiteral$decimalFormat") else DecimalFormat(decimalFormat)
        return shortFormat to longFormat
    }

    /**
     * @return receiver wrapped in '' if non blank, empty string otherwise
     */
    private fun String.literal() = takeIf(String::isNotBlank)
        ?.let { "'$this'" }
        ?: ""
}
