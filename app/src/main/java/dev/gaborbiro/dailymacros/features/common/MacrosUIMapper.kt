package dev.gaborbiro.dailymacros.features.common

import android.icu.text.DecimalFormat
import android.util.Range
import dev.gaborbiro.dailymacros.design.DailyMacrosColors
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelMacroProgress
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem
import dev.gaborbiro.dailymacros.features.common.model.MacrosUIModel
import dev.gaborbiro.dailymacros.repo.records.domain.model.Macros
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.settings.model.Target
import dev.gaborbiro.dailymacros.repo.settings.model.Targets
import java.time.LocalDate

internal class MacrosUIMapper(
    private val dateUIMapper: DateUIMapper,
) {

    fun mapMacroProgressTable(
        records: List<Record>,
        targets: Targets,
        date: LocalDate,
    ): ListUIModelMacroProgress {
        val totalCalories = records.sumOf { it.template.macros?.calories ?: 0 }.toFloat()
        val totalProtein =
            records.sumOf { it.template.macros?.protein?.toDouble() ?: 0.0 }.toFloat()
        val totalCarbs =
            records.sumOf { it.template.macros?.carbohydrates?.toDouble() ?: 0.0 }.toFloat()
        val totalSugar =
            records.sumOf { it.template.macros?.ofWhichSugar?.toDouble() ?: 0.0 }.toFloat()
        val totalFat = records.sumOf { it.template.macros?.fat?.toDouble() ?: 0.0 }.toFloat()
        val totalSaturated =
            records.sumOf { it.template.macros?.ofWhichSaturated?.toDouble() ?: 0.0 }.toFloat()
        val totalSalt = records.sumOf { it.template.macros?.salt?.toDouble() ?: 0.0 }.toFloat()
        val totalFibre = records.sumOf { it.template.macros?.fibre?.toDouble() ?: 0.0 }.toFloat()

        fun progress(target: Target, total: Float) = target.max?.let { total / it }

        fun targetRange(target: Target): Range<Float> {
            return Range(
                if (target.min != null && target.max != null) target.min.toFloat() / target.max.toFloat() else 0f,
                1f,
            )
        }

        fun gramRangeLabel(target: Target): String {
            return "${target.min ?: "?"}-${target.max ?: "?"}g"
        }

        val progress = buildList {
            targets.calories.takeIf { it.enabled }?.let {
                val min = if (it.min != null) {
                    if (it.min < 1000) {
                        it.min.toString()
                    } else {
                        DecimalFormat("#.#").format(it.min / 1000f)
                    }
                } else {
                    "?"
                }
                val max = if (it.max != null) {
                    if (it.max < 1000) {
                        it.max.toString()
                    } else {
                        DecimalFormat("#.#").format(it.max / 1000f)
                    }
                } else {
                    "?"
                }
                val rangeLabel = "$min-${max}k"
                add(
                    MacroProgressItem(
                        title = "Calories",
                        progress0to1 = progress(it, totalCalories) ?: 0f,
                        progressLabel = mapCalories(totalCalories, withLabel = false)!!,
                        targetRange0to1 = targetRange(it),
                        rangeLabel = rangeLabel,
                        color = DailyMacrosColors.calorieColor,
                    )
                )
            }
            targets.protein.takeIf { it.enabled }?.let {
                add(
                    MacroProgressItem(
                        title = "Protein",
                        progress0to1 = progress(it, totalProtein) ?: 0f,
                        progressLabel = mapProtein(totalProtein, withLabel = false) ?: "0g",
                        targetRange0to1 = targetRange(it),
                        rangeLabel = gramRangeLabel(it),
                        color = DailyMacrosColors.proteinColor,
                    )
                )
            }
            targets.salt.takeIf { it.enabled }?.let {
                add(
                    MacroProgressItem(
                        title = "Salt",
                        progress0to1 = progress(it, totalSalt) ?: 0f,
                        progressLabel = mapSalt(totalSalt, withLabel = false) ?: "0.0g",
                        targetRange0to1 = targetRange(it),
                        rangeLabel = gramRangeLabel(it),
                        color = DailyMacrosColors.saltColor,
                    )
                )
            }
            targets.fibre.takeIf { it.enabled }?.let {
                add(
                    MacroProgressItem(
                        title = "Fibre",
                        progress0to1 = progress(it, totalFibre) ?: 0f,
                        progressLabel = mapFibre(totalFibre, withLabel = false) ?: "0g",
                        targetRange0to1 = targetRange(it),
                        rangeLabel = gramRangeLabel(it),
                        color = DailyMacrosColors.fibreColor,
                    )
                )
            }
            targets.carbs.takeIf { it.enabled }?.let {
                add(
                    MacroProgressItem(
                        title = "Carbs",
                        progress0to1 = progress(it, totalCarbs) ?: 0f,
                        progressLabel = mapCarbs(totalCarbs, sugar = null, withLabel = false)
                            ?: "0g",
                        targetRange0to1 = targetRange(it),
                        rangeLabel = gramRangeLabel(it),
                        color = DailyMacrosColors.carbsColor,
                    )
                )
            }
            targets.ofWhichSugar.takeIf { it.enabled }?.let {
                add(
                    MacroProgressItem(
                        title = "sugar",
                        progress0to1 = progress(it, totalSugar) ?: 0f,
                        progressLabel = mapSugar(totalSugar, withLabel = false) ?: "0g",
                        targetRange0to1 = targetRange(it),
                        rangeLabel = gramRangeLabel(it),
                        color = DailyMacrosColors.carbsColor,
                    )
                )
            }
            targets.fat.takeIf { it.enabled }?.let {
                add(
                    MacroProgressItem(
                        title = "Fat",
                        progress0to1 = progress(it, totalFat) ?: 0f,
                        progressLabel = mapFat(totalFat, saturated = null, withLabel = false)
                            ?: "0g",
                        targetRange0to1 = targetRange(it),
                        rangeLabel = gramRangeLabel(it),
                        color = DailyMacrosColors.fatColor,
                    )
                )
            }
            targets.ofWhichSaturated.takeIf { it.enabled }?.let {
                add(
                    MacroProgressItem(
                        title = "saturated",
                        progress0to1 = progress(it, totalSaturated) ?: 0f,
                        progressLabel = mapSaturated(totalSaturated, withLabel = false) ?: "0g",
                        targetRange0to1 = targetRange(it),
                        rangeLabel = gramRangeLabel(it),
                        color = DailyMacrosColors.fatColor,
                    )
                )
            }
        }

        return ListUIModelMacroProgress(
            listItemId = date.toEpochDay(),
            dayTitle = dateUIMapper.map(date),
            progress = progress,
        )
    }

    fun mapMacrosString(macros: Macros?, isShort: Boolean = false): String? {
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

    fun mapMacros(macros: Macros): MacrosUIModel {
        return MacrosUIModel(
            calories = mapCalories(macros.calories, isShort = true, withLabel = true),
            protein = mapProtein(macros.protein, isShort = true, withLabel = true),
            fat = mapFat(macros.fat, macros.ofWhichSaturated, isShort = true, withLabel = true),
            carbs = mapCarbs(macros.carbohydrates, macros.ofWhichSugar, isShort = true, withLabel = true),
            salt = mapSalt(macros.salt, isShort = true, withLabel = true),
            fibre = mapFibre(macros.fibre, isShort = true, withLabel = true),
        )
    }

    fun mapCalories(
        value: Number?,
        isShort: Boolean = false,
        withLabel: Boolean = true,
    ): String? {
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "",  // no short label because "cal" makes this macro recognisable enough
            longLabel = "Calories:",
            unit = "kcal",
            withLabel = withLabel,
            smallScaleValue = smallScaleValue,
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
            shortLabel = "Protein",
            longLabel = "Protein:",
            unit = if (isShort) "" else "g",
            withLabel = withLabel,
            smallScaleValue = smallScaleValue,
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
            shortLabel = "Carbs",
            longLabel = "Carbs:",
            unit = if (isShort) "" else "g",
            withLabel = withLabel,
            smallScaleValue = smallScaleValue,
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
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "",  // no short label because in short mode sugar is displayed after carbs in parenthesis
            longLabel = "of which sugar:",
            unit = if (isShort) "" else "g",
            withLabel = withLabel,
            smallScaleValue = smallScaleValue,
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
            shortLabel = "Fat",
            longLabel = "Fat:",
            unit = if (isShort) "" else "g",
            withLabel = withLabel,
            smallScaleValue = smallScaleValue,
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
            shortLabel = "", // no short label because in short mode saturated fats are displayed after fat in parenthesis
            longLabel = "of which saturated:",
            unit = if (isShort) "" else "g",
            withLabel = withLabel,
            smallScaleValue = smallScaleValue,
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
            shortLabel = "Salt",
            longLabel = "Salt:",
            unit = if (isShort) "" else "g",
            withLabel = withLabel,
            smallScaleValue = smallScaleValue,
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
            shortLabel = "Fibre",
            longLabel = "Fibre:",
            unit = if (isShort) "" else "g",
            withLabel = withLabel,
            smallScaleValue = smallScaleValue,
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

    private fun decimalFormat(decimals: Int, fixed: Boolean = false): String {
        require(decimals >= 0)
        val pattern = buildString {
            append("0")
            if (decimals > 0) {
                append('.')
                val ch = if (fixed) '0' else '#'
                repeat(decimals) { append(ch) }
            }
        }
        return pattern
    }

    private fun generateFormats(
        shortLabel: String,
        longLabel: String,
        unit: String,
        withLabel: Boolean,
        smallScaleValue: Boolean,
    ): Pair<SafeNumberFormatter, SafeNumberFormatter> {
        val unitLiteral = unit.literal()
        val decimals = if (smallScaleValue) 1 else 0
        val longDecimalFormat = "${decimalFormat(decimals)}$unitLiteral"
        val shortDecimalFormat = if (smallScaleValue) longDecimalFormat else "#$unitLiteral"
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
