package dev.gaborbiro.dailymacros.features.common

import android.icu.text.DecimalFormat
import android.util.Range
import dev.gaborbiro.dailymacros.features.common.model.DailySummaryEntry
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelDailySummary
import dev.gaborbiro.dailymacros.features.common.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.repo.records.domain.model.NutrientsBreakdown
import dev.gaborbiro.dailymacros.repo.settings.model.Target
import dev.gaborbiro.dailymacros.repo.settings.model.Targets
import kotlin.math.absoluteValue

internal class NutrientsUIMapper(
    private val dateUIMapper: DateUIMapper,
) {

    fun mapDailyNutrientProgressTable(
        day: TravelDay,
        targets: Targets,
    ): ListUiModelDailySummary {
        val records = day.records
        val totalCalories = records.sumOf { it.template.nutrientsBreakdown?.calories ?: 0 }
        val totalProtein =
            records.sumOf { it.template.nutrientsBreakdown?.protein?.toDouble() ?: 0.0 }.toFloat()
        val totalCarbs =
            records.sumOf { it.template.nutrientsBreakdown?.carbs?.toDouble() ?: 0.0 }.toFloat()
        val totalSugar =
            records.sumOf { it.template.nutrientsBreakdown?.ofWhichSugar?.toDouble() ?: 0.0 }.toFloat()
        val totalAddedSugar =
            records.sumOf { it.template.nutrientsBreakdown?.ofWhichAddedSugar?.toDouble() ?: 0.0 }.toFloat()
        val totalFat = records.sumOf { it.template.nutrientsBreakdown?.fat?.toDouble() ?: 0.0 }.toFloat()
        val totalSaturated =
            records.sumOf { it.template.nutrientsBreakdown?.ofWhichSaturated?.toDouble() ?: 0.0 }.toFloat()
        val totalSalt = records.sumOf { it.template.nutrientsBreakdown?.salt?.toDouble() ?: 0.0 }.toFloat()
        val totalFibre = records.sumOf { it.template.nutrientsBreakdown?.fibre?.toDouble() ?: 0.0 }.toFloat()

        val totalNutrientsBreakdown = NutrientsBreakdown(
            calories = totalCalories,
            protein = totalProtein,
            fat = totalFat,
            ofWhichSaturated = totalSaturated,
            carbs = totalCarbs,
            ofWhichSugar = totalSugar,
            ofWhichAddedSugar = totalAddedSugar,
            salt = totalSalt,
            fibre = totalFibre,
            notes = null,
        )

        val progressItems = buildNutrientProgressItems(totalNutrientsBreakdown, targets)

        val infoMessage = buildTimezoneInfo(day)

        return ListUiModelDailySummary(
            listItemId = day.day.atStartOfDay(day.startZone).toInstant().toEpochMilli(),
            dayTitle = dateUIMapper.mapDayTitleTimestamp(day.day),
            infoMessage = infoMessage,
            entries = progressItems,
        )
    }

    fun buildNutrientProgressItems(
        nutrientsBreakdown: NutrientsBreakdown,
        targets: Targets,
    ): List<DailySummaryEntry> {
        fun gramRangeLabel(target: Target): String =
            "${target.min ?: "?"}-${target.max ?: "?"}"

        return buildList {
            targets.calories.takeIf { it.enabled }?.let {
                val min = if (it.min != null) {
                    DecimalFormat(".#").format(it.min / 1000f)
                } else {
                    "?"
                }
                val max = if (it.max != null) {
                    DecimalFormat(".#").format(it.max / 1000f)
                } else {
                    "?"
                }
                val rangeLabel = "${min}k-${max}k"
                add(
                    DailySummaryEntry(
                        title = "Calories",
                        progress0to1 = targetProgress(it, nutrientsBreakdown.calories?.toFloat() ?: 0f) ?: 0f,
                        progressLabel = formatCalories(nutrientsBreakdown.calories, isShort = false, withLabel = false) ?: "0",
                        targetRange0to1 = targetRange(it),
                        targetRangeLabel = rangeLabel,
                        color = { it.calorieColor },
                    )
                )
            }
            targets.protein.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "Protein",
                        progress0to1 = targetProgress(it, nutrientsBreakdown.protein ?: 0f) ?: 0f,
                        progressLabel = formatProtein(nutrientsBreakdown.protein, isShort = false, withLabel = false) ?: "0g",
                        targetRange0to1 = targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.proteinColor },
                    )
                )
            }
            targets.salt.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "Salt",
                        progress0to1 = targetProgress(it, nutrientsBreakdown.salt ?: 0f) ?: 0f,
                        progressLabel = formatSalt(nutrientsBreakdown.salt, isShort = false, withLabel = false) ?: "0.0g",
                        targetRange0to1 = targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.saltColor },
                    )
                )
            }
            targets.fibre.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "Fibre",
                        progress0to1 = targetProgress(it, nutrientsBreakdown.fibre ?: 0f) ?: 0f,
                        progressLabel = formatFibre(nutrientsBreakdown.fibre, isShort = false, withLabel = false) ?: "0g",
                        targetRange0to1 = targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.fibreColor },
                    )
                )
            }
            targets.carbs.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "Carbs",
                        progress0to1 = targetProgress(it, nutrientsBreakdown.carbs ?: 0f) ?: 0f,
                        progressLabel = formatCarbs(nutrientsBreakdown.carbs, sugar = null, addedSugar = null, isShort = false, withLabel = false)
                            ?: "0g",
                        targetRange0to1 = targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.carbsColor },
                    )
                )
            }
            targets.ofWhichSugar.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "sugar",
                        progress0to1 = targetProgress(it, nutrientsBreakdown.ofWhichSugar ?: 0f) ?: 0f,
                        progressLabel = formatSugar(nutrientsBreakdown.ofWhichSugar, isShort = false, withLabel = false) ?: "0g",
                        targetRange0to1 = targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.carbsColor },
                    )
                )
            }
            targets.fat.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "Fat",
                        progress0to1 = targetProgress(it, nutrientsBreakdown.fat ?: 0f) ?: 0f,
                        progressLabel = formatFat(nutrientsBreakdown.fat, saturated = null, isShort = false, withLabel = false)
                            ?: "0g",
                        targetRange0to1 = targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.fatColor },
                    )
                )
            }
            targets.ofWhichSaturated.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "saturated",
                        progress0to1 = targetProgress(it, nutrientsBreakdown.ofWhichSaturated ?: 0f) ?: 0f,
                        progressLabel = formatSaturatedFat(nutrientsBreakdown.ofWhichSaturated, isShort = false, withLabel = false) ?: "0g",
                        targetRange0to1 = targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.fatColor },
                    )
                )
            }
        }
    }

    private fun buildTimezoneInfo(day: TravelDay): String? {
        val startZone = day.startZone
        val endZone = day.endZone
        if (startZone == endZone) return null

        val deltaHours = day.duration.toHours() - 24
        val percent = (deltaHours / 24f * 100).toInt()

        if (deltaHours.absoluteValue <= 2) return null

        val direction = if (deltaHours > 0) "longer" else "shorter"
        val advice = if (deltaHours > 0) {
            "This means the day's events are pushed back (end of the day, your bedtime, meals...). " +
                    "Try to go to bed later, when locals do (but don't drink coffee after 5pm local time)." +
                    "Try to follow local meal-times. Don't skip local dinner, just because " +
                    "you already had dinner on the airplane. Restaurants might be closed for " +
                    "the night. Try to have smaller meals leading up to your arrival, to prevent over-eating."
        } else {
            "This means the day's events get brought up (end of the day, your bedtime, meals...). " +
                    "Try to go to bed earlier, when locals do. If the difference is extreme, " +
                    "expect a few days of adjustment. For example in case of an 8 hour jet " +
                    "lag, on the first day go to bed 6 hours after locals do, then 4 hours, then 2..."
        }

        return buildString {
            append("\uD83D\uDCA1 Due to timezone change this day is $deltaHours hrs $direction ($percent%).\n")
            append(advice)
        }
    }

    fun mapMacrosPrintout(nutrientsBreakdown: NutrientsBreakdown?, isShort: Boolean = false): String? {
        return listOfNotNull(
            nutrientsBreakdown?.calories?.let { formatCalories(it, isShort, withLabel = true) },
            nutrientsBreakdown?.protein?.let { formatProtein(it, isShort, withLabel = true) },
            nutrientsBreakdown?.fat?.let { formatFat(it, nutrientsBreakdown.ofWhichSaturated, isShort, withLabel = true) },
            if (!isShort) {
                nutrientsBreakdown?.ofWhichSaturated?.let { formatSaturatedFat(it, isShort = false, withLabel = true) }
            } else null,
            nutrientsBreakdown?.carbs?.let { formatCarbs(it, nutrientsBreakdown.ofWhichSugar, nutrientsBreakdown.ofWhichAddedSugar, isShort, withLabel = true) },
            if (!isShort) {
                nutrientsBreakdown?.ofWhichSugar?.let { formatSugar(it, isShort = false, withLabel = true) }
            } else null,
            if (!isShort) {
                nutrientsBreakdown?.ofWhichAddedSugar?.let { formatAddedSugar(it, isShort = false, withLabel = true) }
            } else null,
            nutrientsBreakdown?.salt?.let { formatSalt(it, isShort, withLabel = true) },
            nutrientsBreakdown?.fibre?.let { formatFibre(it, isShort, withLabel = true) }
        )
            .joinToString()
            .takeIf { it.isNotBlank() }
    }

    fun mapMacroAmounts(nutrientsBreakdown: NutrientsBreakdown): NutrientsUiModel {
        return NutrientsUiModel(
            calories = formatCalories(nutrientsBreakdown.calories, isShort = true, withLabel = true),
            protein = formatProtein(nutrientsBreakdown.protein, isShort = true, withLabel = true),
            fat = formatFat(nutrientsBreakdown.fat, nutrientsBreakdown.ofWhichSaturated, isShort = true, withLabel = true),
            carbs = formatCarbs(nutrientsBreakdown.carbs, nutrientsBreakdown.ofWhichSugar, nutrientsBreakdown.ofWhichAddedSugar, isShort = true, withLabel = true),
            salt = formatSalt(nutrientsBreakdown.salt, isShort = true, withLabel = true),
            fibre = formatFibre(nutrientsBreakdown.fibre, isShort = true, withLabel = true),
        )
    }

    fun formatCalories(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "",  // no short label because "kcal" makes this value recognizable enough
            longLabel = "Calories:",
            unit = "kcal",
            withLabel = withLabel,
            allowDecimal = smallScaleValue,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort, allowDecimal = smallScaleValue)
    }

    fun formatProtein(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "Protein",
            longLabel = "Protein:",
            unit = "g",
            withLabel = withLabel,
            allowDecimal = smallScaleValue,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort, allowDecimal = smallScaleValue)
    }

    fun formatCarbs(
        value: Number?,
        sugar: Number?,
        addedSugar: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "Carb",
            longLabel = "Carb:",
            unit = "g",
            withLabel = withLabel,
            allowDecimal = smallScaleValue,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort, allowDecimal = smallScaleValue) {
            val sugar = formatSugar(sugar, isShort, withLabel = false)
            val addedSugar = formatAddedSugar(addedSugar, isShort, withLabel = false)
            sugar?.let {
                "$sugar${addedSugar?.let { "/$it" } ?: ""}"
            }
        }
    }

    fun formatSugar(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "",  // no short label because in short mode sugar is displayed after carbs in parenthesis
            longLabel = "of which sugar:",
            unit = "g",
            withLabel = withLabel,
            allowDecimal = smallScaleValue,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort, allowDecimal = smallScaleValue)
    }

    fun formatAddedSugar(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "",  // no short label because in short mode sugar is displayed after carbs in parenthesis
            longLabel = "of which added sugar:",
            unit = "g",
            withLabel = withLabel,
            allowDecimal = smallScaleValue,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort, allowDecimal = smallScaleValue)
    }

    fun formatFat(
        value: Number?,
        saturated: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "Fat",
            longLabel = "Fat:",
            unit = "g",
            withLabel = withLabel,
            allowDecimal = smallScaleValue,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort, allowDecimal = smallScaleValue) {
            formatSaturatedFat(saturated, isShort, withLabel = false)
        }
    }

    fun formatSaturatedFat(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "", // no short label because in short mode saturated fats are displayed after fat in parenthesis
            longLabel = "of which saturated:",
            unit = "g",
            withLabel = withLabel,
            allowDecimal = smallScaleValue,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort, allowDecimal = smallScaleValue)
    }

    fun formatSalt(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        val smallScaleValue = true
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "Salt",
            longLabel = "Salt:",
            unit = "g",
            withLabel = withLabel,
            allowDecimal = smallScaleValue,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort, allowDecimal = smallScaleValue)
    }

    fun formatFibre(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        val smallScaleValue = false
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "Fibre",
            longLabel = "Fibre:",
            unit = "g",
            withLabel = withLabel,
            allowDecimal = smallScaleValue,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort, allowDecimal = smallScaleValue)
    }

    fun targetProgress(target: Target, total: Float): Float? = target.max?.let { total / it }

    fun targetRange(target: Target): Range<Float> =
        Range(
            /* lower = */ if (target.min != null && target.max != null) {
                target.min.toFloat() / target.max.toFloat()
            } else {
                0f
            },
            /* upper = */ 1f
        )

    private fun formatMacroAmount(
        value: Number?,
        shortFormat: SafeNumberFormatter,
        longFormat: SafeNumberFormatter,
        isShort: Boolean = false,
        allowDecimal: Boolean,
        contractedValue: (() -> String?)? = null,
    ): String? {
        return value
            ?.takeIf {
                if (allowDecimal) true else it.toInt() > 0f
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
        allowDecimal: Boolean,
    ): Pair<SafeNumberFormatter, SafeNumberFormatter> {
        val unitLiteral = unit.literal()
        val decimals = if (allowDecimal) 1 else 0
        val longDecimalFormat = "${decimalFormat(decimals)}$unitLiteral"
        val shortDecimalFormat = if (allowDecimal) longDecimalFormat else "#$unitLiteral"
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
