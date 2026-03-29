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
import kotlin.math.pow

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

        val progressItems = buildDailyNutrientProgressItems(totalNutrientBreakdown, targets)

        val infoMessage = buildTimezoneInfo(day)

        return ListUiModelDailySummary(
            listItemId = day.day.atStartOfDay(day.startZone).toInstant().toEpochMilli(),
            dayTitle = mapDayTitleTimestamp(day.day),
            infoMessage = infoMessage,
            entries = progressItems,
        )
    }

    fun buildDailyNutrientProgressItems(
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
                        progressLabel = formatCalories(nutrientBreakdown.calories, isShort = true, withLabel = false) ?: "0",
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
                        progressLabel = formatProtein(nutrientBreakdown.protein, isShort = true, withLabel = false) ?: "0g",
                        targetRange0to1 = targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.proteinColor },
                    )
                )
            }
            targets.fat.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "Fat",
                        progress0to1 = targetProgress(it, nutrientBreakdown.fat ?: 0f) ?: 0f,
                        progressLabel = formatFat(nutrientBreakdown.fat, saturated = null, isShort = true, withLabel = false)
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
                        progressLabel = formatSaturatedFat(nutrientBreakdown.ofWhichSaturated, isShort = true, withLabel = false) ?: "0g",
                        targetRange0to1 = targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.fatColor },
                    )
                )
            }
            targets.carbs.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "Carbs",
                        progress0to1 = targetProgress(it, nutrientBreakdown.carbs ?: 0f) ?: 0f,
                        progressLabel = formatCarbs(nutrientBreakdown.carbs, sugar = null, addedSugar = null, isShort = true, withLabel = false)
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
                        progressLabel = formatSugar(nutrientBreakdown.ofWhichSugar, isShort = true, withLabel = false) ?: "0g",
                        targetRange0to1 = targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.carbsColor },
                    )
                )
            }
            targets.salt.takeIf { it.enabled }?.let {
                add(
                    DailySummaryEntry(
                        title = "Salt",
                        progress0to1 = targetProgress(it, nutrientBreakdown.salt ?: 0f) ?: 0f,
                        progressLabel = formatSalt(nutrientBreakdown.salt, isShort = true, withLabel = false) ?: "0.00g",
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
                        progressLabel = formatFibre(nutrientBreakdown.fibre, isShort = true, withLabel = false) ?: "0g",
                        targetRange0to1 = targetRange(it),
                        targetRangeLabel = gramRangeLabel(it),
                        color = { it.fibreColor },
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

    fun mapRecordNutrients(nutrientBreakdown: TemplateNutrientBreakdown): NutrientsUiModel {
        return NutrientsUiModel(
            calories = formatCalories(nutrientBreakdown.calories, isShort = true, withLabel = false),
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
        val label = if (withLabel) (if (isShort) "Calories" else "Calories:") else null
        val format = generateFormat(
            label = label,
            unit = "kcal",
            decimalCount = NutrientDisplayLine.Calories.decimalCount,
        )
        return formatMacroAmount(value, format)
    }

    fun formatProtein(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val label = if (withLabel) (if (isShort) "Protein" else "Protein:") else null
        val format = generateFormat(
            label = label,
            unit = "g",
            decimalCount = NutrientDisplayLine.Protein.decimalCount,
        )
        return formatMacroAmount(value, format)
    }

    fun formatCarbs(
        value: Number?,
        sugar: Number?,
        addedSugar: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val label = if (withLabel) (if (isShort) "Carb" else "Carb:") else null
        val format = generateFormat(
            label = label,
            unit = "g",
            decimalCount = NutrientDisplayLine.Carb.decimalCount,
        )
        return formatMacroAmount(value, format) {
            sugar?.let {
                val sugar = formatSugar(sugar, isShort = true, withLabel = false)
                val addedSugar = addedSugar?.let {
                    formatAddedSugar(addedSugar, isShort = true, withLabel = false)
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
        // no short label because in short mode sugar is displayed after carbs in parenthesis
        val label = if (withLabel) (if (isShort) "" else "of which sugar:") else null
        val format = generateFormat(
            label = label,
            unit = "g",
            decimalCount = NutrientDisplayLine.OfWhichSugar.decimalCount,
        )
        return formatMacroAmount(value, format)
    }

    fun formatAddedSugar(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val label = if (withLabel) (if (isShort) "" else "of which added sugar:") else null
        // no short label because in short mode sugar is displayed after carbs in parenthesis
        val format = generateFormat(
            label = label,
            unit = "g",
            decimalCount = NutrientDisplayLine.OfWhichAddedSugar.decimalCount,
        )
        return formatMacroAmount(value, format)
    }

    fun formatFat(
        value: Number?,
        saturated: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val label = if (withLabel) (if (isShort) "Fat" else "Fat:") else null
        val format = generateFormat(
            label = label,
            unit = "g",
            decimalCount = NutrientDisplayLine.Fat.decimalCount,
        )
        return formatMacroAmount(value, format) {
            saturated?.let {
                formatSaturatedFat(saturated, isShort = true, withLabel = false)
            }
        }
    }

    fun formatSaturatedFat(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        // no short label because in short mode saturated fats are displayed after fat in parenthesis
        val label = if (withLabel) (if (isShort) "" else "of which saturated:") else null
        val format = generateFormat(
            label = label,
            unit = "g",
            decimalCount = NutrientDisplayLine.OfWhichSaturated.decimalCount,
        )
        return formatMacroAmount(value, format)
    }

    fun formatSalt(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val label = if (withLabel) (if (isShort) "Salt" else "Salt:") else null
        val format = generateFormat(
            label = label,
            unit = "g",
            decimalCount = NutrientDisplayLine.Salt.decimalCount,
        )
        return formatMacroAmount(value, format)
    }

    fun formatFibre(
        value: Number?,
        isShort: Boolean,
        withLabel: Boolean,
    ): String? {
        value ?: return null
        val label = if (withLabel) (if (isShort) "Fibre" else "Fibre:") else null
        val format = generateFormat(
            label = label,
            unit = "g",
            decimalCount = NutrientDisplayLine.Fibre.decimalCount,
        )
        return formatMacroAmount(value, format)
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
        unit: String,
        decimalCount: Int,
    ): SafeNumberFormatter {
        val decimalPattern = "${decimalFormat(decimalCount)}${unit.literal()}"


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

    private fun mapDayTitleTimestamp(localDate: LocalDate): String {
        return localDate.format(DateTimeFormatter.ofPattern("E, dd MMM"))
    }
}
