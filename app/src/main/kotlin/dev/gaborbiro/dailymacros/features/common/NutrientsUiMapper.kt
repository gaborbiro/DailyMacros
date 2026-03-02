package dev.gaborbiro.dailymacros.features.common

import android.icu.text.DecimalFormat
import android.util.Range
import dev.gaborbiro.dailymacros.features.common.model.DailySummaryEntry
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelDailySummary
import dev.gaborbiro.dailymacros.features.common.model.NutrientBreakdown
import dev.gaborbiro.dailymacros.features.common.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.features.common.model.TravelDay
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

internal class NutrientsUiMapper {

    fun mapDailyNutrientProgressTable(
        day: TravelDay,
        targets: Targets,
    ): ListUiModelDailySummary {
        val records = day.records
        val totalCalories = records.sumOf { it.template.nutrients.calories ?: 0 }
        val totalProtein =
            records.sumOf { it.template.nutrients.protein?.toDouble() ?: 0.0 }.toFloat()
        val totalCarbs =
            records.sumOf { it.template.nutrients.carbs?.toDouble() ?: 0.0 }.toFloat()
        val totalSugar =
            records.sumOf { it.template.nutrients.ofWhichSugar?.toDouble() ?: 0.0 }.toFloat()
        val totalFat = records.sumOf { it.template.nutrients.fat?.toDouble() ?: 0.0 }.toFloat()
        val totalSaturated =
            records.sumOf { it.template.nutrients.ofWhichSaturated?.toDouble() ?: 0.0 }.toFloat()
        val totalSalt = records.sumOf { it.template.nutrients.salt?.toDouble() ?: 0.0 }.toFloat()
        val totalFibre = records.sumOf { it.template.nutrients.fibre?.toDouble() ?: 0.0 }.toFloat()

        val totalNutrientBreakdown = NutrientBreakdown(
            calories = totalCalories,
            protein = totalProtein,
            fat = totalFat,
            ofWhichSaturated = totalSaturated,
            carbs = totalCarbs,
            ofWhichSugar = totalSugar,
            // added sugar is not displayed in daily summary
            salt = totalSalt,
            fibre = totalFibre,
        )

        val progressItems = buildNutrientProgressItems(totalNutrientBreakdown, targets)

        val infoMessage = buildTimezoneInfo(day)

        return ListUiModelDailySummary(
            listItemId = day.day.atStartOfDay(day.startZone).toInstant().toEpochMilli(),
            dayTitle = mapDayTitleTimestamp(day.day),
            infoMessage = infoMessage,
            entries = progressItems,
        )
    }

    fun buildNutrientProgressItems(
        nutrientBreakdown: NutrientBreakdown,
        targets: Targets,
    ): List<DailySummaryEntry> {
        fun gramRangeLabel(target: Target): String =
            "${target.min ?: "?"}-${target.max ?: "?"}"

        return buildList {
            targets.calories.takeIf { it.enabled }?.let {
                val minVal = it.min
                val maxVal = it.max
                val min = if (minVal != null) {
                    DecimalFormat(".#").format(minVal / 1000f)
                } else {
                    "?"
                }
                val max = if (maxVal != null) {
                    DecimalFormat(".#").format(maxVal / 1000f)
                } else {
                    "?"
                }
                val rangeLabel = "${min}k-${max}k"
                add(
                    DailySummaryEntry(
                        title = "Calories",
                        progress0to1 = targetProgress(it, nutrientBreakdown.calories?.toFloat() ?: 0f) ?: 0f,
                        progressLabel = formatCalories(nutrientBreakdown.calories, isShort = false, withLabel = false) ?: "0",
                        targetRange0to1 = targetRange(it),
                        targetRangeLabel = rangeLabel,
                        color = { it.caloriesColor },
                    )
                )
            }
            targets.protein.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "Protein",
                        progress0to1 = targetProgress(it, nutrientBreakdown.protein ?: 0f) ?: 0f,
                        progressLabel = formatProtein(nutrientBreakdown.protein, isShort = false, withLabel = false) ?: "0g",
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
                        progress0to1 = targetProgress(it, nutrientBreakdown.salt ?: 0f) ?: 0f,
                        progressLabel = formatSalt(nutrientBreakdown.salt, isShort = false, withLabel = false) ?: "0.00g",
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
                        progress0to1 = targetProgress(it, nutrientBreakdown.fibre ?: 0f) ?: 0f,
                        progressLabel = formatFibre(nutrientBreakdown.fibre, isShort = false, withLabel = false) ?: "0g",
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
                        progress0to1 = targetProgress(it, nutrientBreakdown.carbs ?: 0f) ?: 0f,
                        progressLabel = formatCarbs(nutrientBreakdown.carbs, sugar = null, addedSugar = null, isShort = false, withLabel = false)
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
                        progress0to1 = targetProgress(it, nutrientBreakdown.ofWhichSugar ?: 0f) ?: 0f,
                        progressLabel = formatSugar(nutrientBreakdown.ofWhichSugar, isShort = false, withLabel = false) ?: "0g",
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
                        progress0to1 = targetProgress(it, nutrientBreakdown.fat ?: 0f) ?: 0f,
                        progressLabel = formatFat(nutrientBreakdown.fat, saturated = null, isShort = false, withLabel = false)
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
                        progress0to1 = targetProgress(it, nutrientBreakdown.ofWhichSaturated ?: 0f) ?: 0f,
                        progressLabel = formatSaturatedFat(nutrientBreakdown.ofWhichSaturated, isShort = false, withLabel = false) ?: "0g",
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

    fun map(nutrientBreakdown: TemplateNutrientBreakdown): NutrientsUiModel {
        return NutrientsUiModel(
            calories = formatCalories(nutrientBreakdown.calories, isShort = true, withLabel = true),
            protein = formatProtein(nutrientBreakdown.protein, isShort = true, withLabel = true),
            fat = formatFat(nutrientBreakdown.fat, nutrientBreakdown.ofWhichSaturated, isShort = true, withLabel = true),
            carbs = formatCarbs(nutrientBreakdown.carbs, nutrientBreakdown.ofWhichSugar, nutrientBreakdown.ofWhichAddedSugar, isShort = true, withLabel = true),
            salt = formatSalt(nutrientBreakdown.salt, isShort = true, withLabel = true),
            fibre = formatFibre(nutrientBreakdown.fibre, isShort = true, withLabel = true),
        )
    }

    fun formatCalories(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "",  // no short label because "kcal" makes this value recognizable enough
            longLabel = "Calories:",
            unit = "kcal",
            withLabel = withLabel,
            decimalCount = 0,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort)
    }

    fun formatProtein(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "Protein",
            longLabel = "Protein:",
            unit = "g",
            withLabel = withLabel,
            decimalCount = 0,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort)
    }

    fun formatCarbs(
        value: Number?,
        sugar: Number?,
        addedSugar: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "Carb",
            longLabel = "Carb:",
            unit = "g",
            withLabel = withLabel,
            decimalCount = 0,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort) {
            sugar?.let {
                val sugar = formatSugar(sugar, isShort, withLabel = false)
                val addedSugar = addedSugar?.let {
                    formatAddedSugar(addedSugar, isShort, withLabel = false)
                }
                "${sugar}${addedSugar?.let { "/$it" } ?: ""}"
            }
        }
    }

    fun formatSugar(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "",  // no short label because in short mode sugar is displayed after carbs in parenthesis
            longLabel = "of which sugar:",
            unit = "g",
            withLabel = withLabel,
            decimalCount = 0,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort)
    }

    fun formatAddedSugar(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "",  // no short label because in short mode sugar is displayed after carbs in parenthesis
            longLabel = "of which added sugar:",
            unit = "g",
            withLabel = withLabel,
            decimalCount = 0,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort)
    }

    fun formatFat(
        value: Number?,
        saturated: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "Fat",
            longLabel = "Fat:",
            unit = "g",
            withLabel = withLabel,
            decimalCount = 0,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort) {
            saturated?.let {
                formatSaturatedFat(saturated, isShort, withLabel = false)
            }
        }
    }

    fun formatSaturatedFat(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "", // no short label because in short mode saturated fats are displayed after fat in parenthesis
            longLabel = "of which saturated:",
            unit = "g",
            withLabel = withLabel,
            decimalCount = 0,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort)
    }

    fun formatSalt(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "Salt",
            longLabel = "Salt:",
            unit = "g",
            withLabel = withLabel,
            decimalCount = 2,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort)
    }

    fun formatFibre(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val (shortFormat, longFormat) = generateFormats(
            shortLabel = "Fibre",
            longLabel = "Fibre:",
            unit = "g",
            withLabel = withLabel,
            decimalCount = 0,
        )
        return formatMacroAmount(value, shortFormat, longFormat, isShort)
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
        shortFormat: SafeNumberFormatter,
        longFormat: SafeNumberFormatter,
        isShort: Boolean = false,
        extraValue: (() -> String?)? = null,
    ): String? {
        val formatted = if (isShort) {
            shortFormat.format(value)
        } else {
            longFormat.format(value)
        }
        return formatted + (extraValue?.invoke()?.let { "($it)" } ?: "")
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
        decimalCount: Int,
    ): Pair<SafeNumberFormatter, SafeNumberFormatter> {
        val unitLiteral = unit.literal()
        val longDecimalFormat = "${decimalFormat(decimalCount)}$unitLiteral"
        val shortDecimalFormat = if (decimalCount > 0) longDecimalFormat else "#$unitLiteral"
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

    fun formatTopContributorText(text: String?): String {
        return text
            ?.takeIf { it.isNotBlank() }
            ?.let {
                "\n    ($it)"
            } ?: ""
    }

    private abstract class SafeNumberFormatter(
        private val format: DecimalFormat,
    ) {
        fun format(value: Number?): String = value?.let { format.format(it) } ?: nullFormat()

        abstract fun nullFormat(): String
    }

    private fun mapDayTitleTimestamp(localDate: LocalDate): String {
        return localDate.format(DateTimeFormatter.ofPattern("E, dd MMM"))
    }
}
