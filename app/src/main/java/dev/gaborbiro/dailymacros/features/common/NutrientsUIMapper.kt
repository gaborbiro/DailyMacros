package dev.gaborbiro.dailymacros.features.common

import android.icu.text.DecimalFormat
import dev.gaborbiro.dailymacros.repo.records.domain.model.Nutrients

internal class NutrientsUIMapper {

    fun map(nutrients: Nutrients?, isShort: Boolean = false): String? {
        return listOfNotNull(
            nutrients?.calories?.let { mapCalories(it, isShort) },
            nutrients?.protein?.let { mapProtein(it, isShort) },
            nutrients?.carbohydrates?.let { mapCarbohydrates(it, nutrients.ofWhichSugar, isShort) },
            if (!isShort) {
                nutrients?.ofWhichSugar?.let { mapSugar(it) }
            } else null,
            nutrients?.fat?.let { mapFat(it, nutrients.ofWhichSaturated, isShort) },
            if (!isShort) {
                nutrients?.ofWhichSaturated?.let { mapSaturated(it) }
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
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "",
            longLabel = "Calories:",
            unit = "cal",
            withLabel = withLabel,
            forceDecimal = smallScaleValue,
        )
        return map(value, shortFormat, longFormat, isShort, forceDecimal = smallScaleValue)
    }

    fun mapProtein(
        value: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "prot",
            longLabel = "Protein:",
            unit = if (isShort) "" else "g",
            withLabel = withLabel,
            forceDecimal = smallScaleValue,
        )
        return map(value, shortFormat, longFormat, isShort, forceDecimal = smallScaleValue)
    }

    fun mapCarbohydrates(
        value: Number?,
        sugar: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "carb",
            longLabel = "Carbs:",
            unit = if (isShort) "" else "g",
            withLabel = withLabel,
            forceDecimal = smallScaleValue,
        )
        return map(value, shortFormat, longFormat, isShort, forceDecimal = smallScaleValue) {
            mapSugar(sugar, isShort, withLabel = false)
        }
    }

    fun mapSugar(
        value: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val smallScaleValue = true
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "",
            longLabel = "of which sugar:",
            unit = if (isShort) "" else "g",
            withLabel = withLabel,
            forceDecimal = smallScaleValue,
        )
        return map(value, shortFormat, longFormat, isShort, forceDecimal = smallScaleValue)
    }

    fun mapFat(
        value: Number?,
        saturated: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "fat",
            longLabel = "Fat:",
            unit = if (isShort) "" else "g",
            withLabel = withLabel,
            forceDecimal = smallScaleValue,
        )
        return map(value, shortFormat, longFormat, isShort, forceDecimal = smallScaleValue) {
            mapSaturated(saturated, isShort, withLabel = false)
        }
    }

    fun mapSaturated(
        value: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "",
            longLabel = "of which saturated:",
            unit = if (isShort) "" else "g",
            withLabel = withLabel,
            forceDecimal = smallScaleValue,
        )
        return map(value, shortFormat, longFormat, isShort, forceDecimal = smallScaleValue)
    }

    fun mapSalt(
        value: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val smallScaleValue = true
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "sal",
            longLabel = "Salt:",
            unit = if (isShort) "" else "g",
            withLabel = withLabel,
            forceDecimal = smallScaleValue,
        )
        return map(value, shortFormat, longFormat, isShort, forceDecimal = smallScaleValue)
    }

    fun mapFibre(
        value: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "fib",
            longLabel = "Fibre:",
            unit = if (isShort) "" else "g",
            withLabel = withLabel,
            forceDecimal = smallScaleValue,
        )
        return map(value, shortFormat, longFormat, isShort, forceDecimal = smallScaleValue)
    }

    private fun map(
        value: Number?,
        shortFormat: DecimalFormat,
        longFormat: DecimalFormat,
        isShort: Boolean = false,
        forceDecimal: Boolean,
        contractedValue: (() -> String?)? = null,
    ): String? {
        return value
            ?.takeIf {
                if (forceDecimal) true else it.toInt() > 0f
            }
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
        shortLabel: String,
        longLabel: String,
        unit: String,
        withLabel: Boolean,
        forceDecimal: Boolean,
    ): Pair<DecimalFormat, DecimalFormat> {
        val unitLiteral = unit.literal()
        val longDecimalFormat = "0.##$unitLiteral"
        val shortDecimalFormat = if (forceDecimal) longDecimalFormat else "#$unitLiteral"
        val shortLabelLiteral = "$shortLabel ".literal()
        val longLabelLiteral = "$longLabel ".literal()
        val shortFormat =
            if (withLabel) DecimalFormat("$shortLabelLiteral$shortDecimalFormat") else DecimalFormat(shortDecimalFormat)
        val longFormat =
            if (withLabel) DecimalFormat("$longLabelLiteral$longDecimalFormat") else DecimalFormat(longDecimalFormat)
        return shortFormat to longFormat
    }

    /**
     * @return receiver wrapped in '' if non blank, [default] otherwise
     */
    private fun String.literal(
        default: String? = "",
    ) = takeIf(String::isNotBlank)
        ?.let { "'$this'" }
        ?: default
}
