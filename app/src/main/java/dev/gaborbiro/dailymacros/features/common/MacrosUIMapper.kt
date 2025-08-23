package dev.gaborbiro.dailymacros.features.common

import android.icu.text.DecimalFormat
import android.util.Range
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressTableUIModel
import dev.gaborbiro.dailymacros.repo.records.domain.model.Macros
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import java.time.LocalDate

internal class MacrosUIMapper {

    fun mapMacroProgressTable(records: List<Record>, date: LocalDate): MacroProgressTableUIModel {
        val totalCalories = records.sumOf { it.template.macros?.calories ?: 0 }
        val totalProtein = records.sumOf { it.template.macros?.protein?.toDouble() ?: 0.0 }
            .toInt()
        val totalCarbs = records.sumOf { it.template.macros?.carbohydrates?.toDouble() ?: 0.0 }
            .toInt()
        val totalSugar = records.sumOf { it.template.macros?.ofWhichSugar?.toDouble() ?: 0.0 }
            .toInt()
        val totalFat = records.sumOf { it.template.macros?.fat?.toDouble() ?: 0.0 }
            .toInt()
        val totalSalt = records.sumOf { it.template.macros?.salt?.toDouble() ?: 0.0 }
            .toInt()
        val totalFibre = records.sumOf { it.template.macros?.fibre?.toDouble() ?: 0.0 }
            .toInt()

        class MacroGoal(
            val name: String,
            val rangeLabel: String,
            val min: Float,
            val max: Float,
            val fuzzy: Boolean,
        ) {
            private val extendedMax = max * 1.1f
            val targetRange = Range(
                min / (if (fuzzy) extendedMax else max),
                max / (if (fuzzy) extendedMax else 1f)
            )

            fun progress(total: Int) = (total / if (fuzzy) extendedMax else max).coerceAtMost(1f)
        }

        val calories = MacroGoal(
            name = "Calories",
            rangeLabel = "2.1-2.2kcal",
            min = 2100f,
            max = 2200f,
            fuzzy = true,
        )
        val protein = MacroGoal(
            name = "Protein",
            rangeLabel = "170-190g",
            min = 170f,
            max = 190f,
            fuzzy = true,
        )
        val fat = MacroGoal(
            name = "Fat",
            rangeLabel = "55-65g",
            min = 55f,
            max = 65f,
            fuzzy = true,
        )
        val carbs = MacroGoal(
            name = "Carbs",
            rangeLabel = "150-200g",
            min = 150f,
            max = 200f,
            fuzzy = true,
        )
        val sugar = MacroGoal(
            name = "Sugar",
            rangeLabel = "<40g ttl., <25g3",
            min = 0f,
            max = 40f,
            fuzzy = false,
        )
        val salt = MacroGoal(
            name = "Salt",
            rangeLabel = "<5g (≈2g Na)",
            min = 0f,
            max = 5f,
            fuzzy = false,
        )
        val fibre = MacroGoal(
            // TODO Women need 21-25g
            name = "Fibre",
            rangeLabel = "30-38g",
            min = 30f,
            max = 38f,
            fuzzy = true,
        )

        val macros = listOf(
            MacroProgressItem(
                title = calories.name,
                progress = calories.progress(totalCalories),
                progressLabel = mapCalories(totalCalories, withLabel = false)!!,
                range = calories.targetRange,
                rangeLabel = calories.rangeLabel,
            ),
            MacroProgressItem(
                title = protein.name,
                progress = protein.progress(totalProtein),
                progressLabel = mapProtein(totalProtein, withLabel = false) ?: "0g",
                range = protein.targetRange,
                rangeLabel = protein.rangeLabel,
            ),
            MacroProgressItem(
                title = fat.name,
                progress = fat.progress(totalFat),
                progressLabel = mapFat(totalFat, null, withLabel = false) ?: "0g",
                range = fat.targetRange,
                rangeLabel = fat.rangeLabel,
            ),
            MacroProgressItem(
                title = carbs.name,
                progress = carbs.progress(totalCarbs),
                progressLabel = mapCarbs(totalCarbs, null, withLabel = false)
                    ?: "0g",
                range = carbs.targetRange,
                rangeLabel = carbs.rangeLabel,
            ),
            MacroProgressItem(
                title = sugar.name,
                progress = sugar.progress(totalSugar),
                progressLabel = mapSugar(totalSugar, withLabel = false) ?: "0g",
                range = sugar.targetRange,
                rangeLabel = sugar.rangeLabel,
            ),
            MacroProgressItem(
                title = salt.name,
                progress = salt.progress(totalSalt),
                progressLabel = mapSalt(totalSalt, withLabel = false) ?: "0g",
                range = salt.targetRange,
                rangeLabel = salt.rangeLabel,
            ),
            MacroProgressItem(
                title = fibre.name,
                progress = fibre.progress(totalFibre),
                progressLabel = mapFibre(totalFibre, withLabel = false) ?: "0g",
                range = fibre.targetRange,
                rangeLabel = fibre.rangeLabel,
            ),
        )

        return MacroProgressTableUIModel(
            date = date,
            macros = macros,
        )
    }

    fun mapAllMacrosLabel(macros: Macros?, isShort: Boolean = false): String? {
        return listOfNotNull(
            macros?.calories?.let { mapCalories(it, isShort) },
            macros?.protein?.let { mapProtein(it, isShort) },
            macros?.fat?.let { mapFat(it, macros.ofWhichSaturated, isShort) },
            if (!isShort) {
                macros?.ofWhichSaturated?.let { mapSaturated(it) }
            } else null,
            macros?.carbohydrates?.let { mapCarbs(it, macros.ofWhichSugar, isShort) },
            if (!isShort) {
                macros?.ofWhichSugar?.let { mapSugar(it) }
            } else null,
            macros?.salt?.let { mapSalt(it, isShort) },
            macros?.fibre?.let { mapFibre(it, isShort) }
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

    fun mapCarbs(
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
        shortFormat: SafeNumberFormatter,
        longFormat: SafeNumberFormatter,
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
                    longFormat.format(value)
                }
            }
    }

    private fun generateFormats(
        shortLabel: String,
        longLabel: String,
        unit: String,
        withLabel: Boolean,
        forceDecimal: Boolean,
    ): Pair<SafeNumberFormatter, SafeNumberFormatter> {
        val unitLiteral = unit.literal()
        val longDecimalFormat = "0.##$unitLiteral"
        val shortDecimalFormat = if (forceDecimal) longDecimalFormat else "#$unitLiteral"
        val shortLabelLiteral = "$shortLabel ".literal()
        val longLabelLiteral = "$longLabel ".literal()
        val shortFormat = if (withLabel) {
            DecimalFormat("$shortLabelLiteral$shortDecimalFormat")
        } else {
            DecimalFormat(shortDecimalFormat)
        }
        val longFormat = if (withLabel) {
            DecimalFormat("$longLabelLiteral$longDecimalFormat")
        } else {
            DecimalFormat(longDecimalFormat)
        }
        val shortSafeFormatter = object : SafeNumberFormatter(shortFormat) {
            override fun nullFormat(): String = shortLabel
        }
        val longSafeFormatter = object : SafeNumberFormatter(longFormat) {
            override fun nullFormat(): String = longLabel
        }
        return shortSafeFormatter to longSafeFormatter
    }

    /**
     * @return receiver wrapped in '' if non blank, [default] otherwise
     */
    private fun String.literal(
        default: String? = "",
    ) = takeIf(String::isNotBlank)
        ?.let { "'$this'" }
        ?: default

    private abstract class SafeNumberFormatter(
        private val format: DecimalFormat,
    ) {
        fun format(value: Number?): String = value?.let { format.format(it) } ?: nullFormat()

        abstract fun nullFormat(): String
    }
}
