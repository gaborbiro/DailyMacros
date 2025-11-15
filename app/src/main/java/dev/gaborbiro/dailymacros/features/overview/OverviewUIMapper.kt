package dev.gaborbiro.dailymacros.features.overview

import android.icu.text.DecimalFormat
import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.common.RecordsUIMapper
import dev.gaborbiro.dailymacros.features.common.TravelDay
import dev.gaborbiro.dailymacros.features.common.model.ChangeDirection
import dev.gaborbiro.dailymacros.features.common.model.ChangeIndicator
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelBase
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelWeeklyReport
import dev.gaborbiro.dailymacros.features.common.model.WeeklySummaryMacroProgressItem
import dev.gaborbiro.dailymacros.repo.records.domain.model.Macros
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.settings.model.Target
import dev.gaborbiro.dailymacros.repo.settings.model.Targets
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

internal class OverviewUIMapper(
    private val recordsUIMapper: RecordsUIMapper,
    private val macrosUIMapper: MacrosUIMapper,
) {
    fun map(
        records: List<Record>,
        targets: Targets,
        showDay: Boolean,
    ): List<ListUIModelBase> {
        val weekFields = WeekFields.of(Locale.getDefault())
        val firstDayOfWeek = weekFields.firstDayOfWeek
        val result = mutableListOf<ListUIModelBase>()
        val currentWeek = mutableListOf<TravelDay>()
        val today = LocalDate.now()

        val grouped = records.groupByWallClockDay()

        grouped.forEachIndexed { index, travelDay ->
            currentWeek += travelDay
            result += travelDay.records.map { recordsUIMapper.map(record = it, forceDay = showDay) }
            result += macrosUIMapper.mapDailyMacroProgressTable(travelDay, targets)

            val lookAhead = grouped.getOrNull(index + 1)
            val weekEnded =
                // Case 1: the travel day is the last day of its week AND it’s fully past today
                (travelDay.day.dayOfWeek == firstDayOfWeek.minus(1) && travelDay.day.isBefore(today)) ||
                        // Case 2: next record belongs to a new week
                        (lookAhead != null && lookAhead.day.get(weekFields.weekOfWeekBasedYear()) != travelDay.day.get(weekFields.weekOfWeekBasedYear())) ||
                        // Case 3: last record, and that week has ended (not current ongoing week)
                        (lookAhead == null && travelDay.day.isBefore(today.with(firstDayOfWeek)))

            if (weekEnded) {
                mapWeeklySummary(currentWeek, targets)?.let { result += it }
                currentWeek.clear()
            }
        }

        return result.reversed()
    }

    private fun mapWeeklySummary(
        days: List<TravelDay>,
        targets: Targets,
    ): ListUIModelWeeklyReport? {
        if (days.isEmpty()) return null

        // 1. Compute total macros per day
        data class DayTotal(
            val macros: Macros,
            val duration: Duration,
        )

        val dailyTotals = days.mapNotNull { day ->
            val dayMacros = day.records.mapNotNull { it.template.macros }
            if (dayMacros.isEmpty()) return@mapNotNull null

            val sum = dayMacros.reduce { acc, m ->
                Macros(
                    calories = (acc.calories ?: 0) + (m.calories ?: 0),
                    protein = (acc.protein ?: 0f) + (m.protein ?: 0f),
                    fat = (acc.fat ?: 0f) + (m.fat ?: 0f),
                    ofWhichSaturated = (acc.ofWhichSaturated ?: 0f) + (m.ofWhichSaturated ?: 0f),
                    carbohydrates = (acc.carbohydrates ?: 0f) + (m.carbohydrates ?: 0f),
                    ofWhichSugar = (acc.ofWhichSugar ?: 0f) + (m.ofWhichSugar ?: 0f),
                    salt = (acc.salt ?: 0f) + (m.salt ?: 0f),
                    fibre = (acc.fibre ?: 0f) + (m.fibre ?: 0f),
                    notes = null,
                )
            }

            DayTotal(sum, day.duration)
        }

        if (dailyTotals.isEmpty()) return null

        // 2. Weighted average helper (by TravelDay duration — long travel days contribute more)
        fun weightedAvg(selector: (Macros) -> Number?): Number? {
            var weightedSum = 0.0
            var totalHours = 0.0
            for (day in dailyTotals) {
                val value = selector(day.macros)?.toDouble() ?: continue
                val hours = day.duration.toHours().coerceAtLeast(1).toDouble()
                weightedSum += value * hours
                totalHours += hours
            }
            return if (totalHours > 0) weightedSum / totalHours else null
        }

        // 3. Compute weekly weighted averages
        val avgMacros = Macros(
            calories = weightedAvg { it.calories }?.toInt(),
            protein = weightedAvg { it.protein }?.toFloat(),
            fat = weightedAvg { it.fat }?.toFloat(),
            ofWhichSaturated = weightedAvg { it.ofWhichSaturated }?.toFloat(),
            carbohydrates = weightedAvg { it.carbohydrates }?.toFloat(),
            ofWhichSugar = weightedAvg { it.ofWhichSugar }?.toFloat(),
            salt = weightedAvg { it.salt }?.toFloat(),
            fibre = weightedAvg { it.fibre }?.toFloat(),
            notes = null,
        )

        // 4. Compare weekly averages against daily targets (no scaling)
        val avgAdherence = calculateAdherence(
            macros = avgMacros,
            targets = targets,
            duration = Duration.ofHours(24) // baseline: daily target
        )

        val weekStart = days.minOf { it.day }

        return ListUIModelWeeklyReport(
            listItemId = weekStart.toEpochDay(),
            weeklyProgress = buildWeeklySummaryMacroItems(avgMacros, targets),
            averageAdherence100Percentage = (avgAdherence * 100).roundToInt()
        )
    }

    /**
     * Builds weekly summary macro items with change indicators.
     * Change is calculated relative to target (showing deviation from target range).
     * Can be extended to support week-over-week comparison by adding previousWeekMacros parameter.
     */
    private fun buildWeeklySummaryMacroItems(
        macros: Macros,
        targets: Targets,
        previousWeekMacros: Macros? = null,
    ): List<WeeklySummaryMacroProgressItem> {
        /**
         * Calculates change indicator based on deviation from target.
         * Positive change = above target (for things like calories, carbs where more is worse)
         * Negative change = below target (for things like protein, fibre where more is better)
         * Can be extended to calculate week-over-week change if previousWeekMacros is provided.
         */
        fun calculateChangeIndicator(
            current: Float?,
            target: Target,
            isMoreBetter: Boolean = false, // true for protein/fibre (more is better), false for calories/carbs/salt (less is better)
        ): ChangeIndicator {
            if (current == null || current == 0f) {
                return ChangeIndicator(ChangeDirection.NEUTRAL, "0%")
            }

            // Calculate deviation from target midpoint
            val targetMid = when {
                target.min != null && target.max != null -> (target.min + target.max) / 2f
                target.max != null -> target.max.toFloat()
                target.min != null -> target.min.toFloat()
                else -> return ChangeIndicator(ChangeDirection.NEUTRAL, "—")
            }

            val deviation = current - targetMid
            val percentChange = if (targetMid > 0) (deviation / targetMid * 100f) else 0f

            // For "more is better" macros (protein, fibre), flip the logic
            val adjustedChange = if (isMoreBetter) -percentChange else percentChange

            val direction = when {
                adjustedChange > 2f -> ChangeDirection.UP
                adjustedChange < -2f -> ChangeDirection.DOWN
                else -> ChangeDirection.NEUTRAL
            }

            val sign = if (adjustedChange > 0) "+" else ""
            val formattedValue = DecimalFormat("#.#").format(adjustedChange.absoluteValue)
            return ChangeIndicator(direction, "$sign$formattedValue%")
        }

        return buildList {
            targets.calories.takeIf { it.enabled }?.let {
                add(
                    WeeklySummaryMacroProgressItem(
                        title = "Calories",
                        progress0to1 = macrosUIMapper.targetProgress(it, macros.calories?.toFloat() ?: 0f) ?: 0f,
                        progressLabel = macrosUIMapper.formatCalories(macros.calories, withLabel = false) ?: "0",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.calories?.toFloat(),
                            target = it,
                            isMoreBetter = false
                        ),
                        color = { it.calorieColor },
                    )
                )
            }
            targets.protein.takeIf { it.enabled }?.let {
                add(
                    WeeklySummaryMacroProgressItem(
                        title = "Protein",
                        progress0to1 = macrosUIMapper.targetProgress(it, macros.protein ?: 0f) ?: 0f,
                        progressLabel = macrosUIMapper.formatProtein(macros.protein, withLabel = false) ?: "0g",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.protein,
                            target = it,
                            isMoreBetter = true
                        ),
                        color = { it.proteinColor },
                    )
                )
            }
            targets.salt.takeIf { it.enabled }?.let {
                add(
                    WeeklySummaryMacroProgressItem(
                        title = "Salt",
                        progress0to1 = macrosUIMapper.targetProgress(it, macros.salt ?: 0f) ?: 0f,
                        progressLabel = macrosUIMapper.formatSalt(macros.salt, withLabel = false) ?: "0.0g",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.salt,
                            target = it,
                            isMoreBetter = false
                        ),
                        color = { it.saltColor },
                    )
                )
            }
            targets.fibre.takeIf { it.enabled }?.let {
                add(
                    WeeklySummaryMacroProgressItem(
                        title = "Fibre",
                        progress0to1 = macrosUIMapper.targetProgress(it, macros.fibre ?: 0f) ?: 0f,
                        progressLabel = macrosUIMapper.formatFibre(macros.fibre, withLabel = false) ?: "0g",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.fibre,
                            target = it,
                            isMoreBetter = true
                        ),
                        color = { it.fibreColor },
                    )
                )
            }
            targets.carbs.takeIf { it.enabled }?.let {
                add(
                    WeeklySummaryMacroProgressItem(
                        title = "Carbs",
                        progress0to1 = macrosUIMapper.targetProgress(it, macros.carbohydrates ?: 0f) ?: 0f,
                        progressLabel = macrosUIMapper.formatCarbs(macros.carbohydrates, sugar = null, withLabel = false)
                            ?: "0g",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.carbohydrates,
                            target = it,
                            isMoreBetter = false
                        ),
                        color = { it.carbsColor },
                    )
                )
            }
            targets.ofWhichSugar.takeIf { it.enabled }?.let {
                add(
                    WeeklySummaryMacroProgressItem(
                        title = "sugar",
                        progress0to1 = macrosUIMapper.targetProgress(it, macros.ofWhichSugar ?: 0f) ?: 0f,
                        progressLabel = macrosUIMapper.formatSugar(macros.ofWhichSugar, withLabel = false) ?: "0g",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.ofWhichSugar,
                            target = it,
                            isMoreBetter = false
                        ),
                        color = { it.carbsColor },
                    )
                )
            }
            targets.fat.takeIf { it.enabled }?.let {
                add(
                    WeeklySummaryMacroProgressItem(
                        title = "Fat",
                        progress0to1 = macrosUIMapper.targetProgress(it, macros.fat ?: 0f) ?: 0f,
                        progressLabel = macrosUIMapper.formatFat(macros.fat, saturated = null, withLabel = false)
                            ?: "0g",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.fat,
                            target = it,
                            isMoreBetter = false
                        ),
                        color = { it.fatColor },
                    )
                )
            }
            targets.ofWhichSaturated.takeIf { it.enabled }?.let {
                add(
                    WeeklySummaryMacroProgressItem(
                        title = "saturated",
                        progress0to1 = macrosUIMapper.targetProgress(it, macros.ofWhichSaturated ?: 0f) ?: 0f,
                        progressLabel = macrosUIMapper.formatSaturatedFat(macros.ofWhichSaturated, withLabel = false) ?: "0g",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.ofWhichSaturated,
                            target = it,
                            isMoreBetter = false
                        ),
                        color = { it.fatColor },
                    )
                )
            }
        }
    }

    private fun calculateAdherence(
        macros: Macros,
        targets: Targets,
        duration: Duration,
    ): Float {
        val scores = mutableListOf<Float>()

        val factor = (duration.toHours().toFloat() / 24f).coerceAtLeast(0.01f) // avoid div-by-zero

        fun score(value: Float?, target: Target): Float? {
            if (!target.enabled || value == null) return null

            val min = target.min?.toFloat()?.times(factor)
            val max = target.max?.toFloat()?.times(factor)

            // No range defined → can't evaluate
            if (min == null && max == null) return null

            // Within scaled range → perfect
            if ((min == null || value >= min) && (max == null || value <= max)) return 1f

            // Below range
            if (min != null && value < min) {
                val diff = min - value
                val span = min.takeIf { it > 0f } ?: 1f
                return (1f - (diff / span)).coerceIn(0f, 1f)
            }

            // Above range
            if (max != null && value > max) {
                val diff = value - max
                val span = max.takeIf { it > 0f } ?: 1f
                return (1f - (diff / span)).coerceIn(0f, 1f)
            }

            return null
        }

        // Evaluate each macro against its scaled targets
        macros.calories?.toFloat()?.let { score(it, targets.calories)?.let(scores::add) }
        macros.protein?.let { score(it, targets.protein)?.let(scores::add) }
        macros.fat?.let { score(it, targets.fat)?.let(scores::add) }
        macros.carbohydrates?.let { score(it, targets.carbs)?.let(scores::add) }
        macros.ofWhichSaturated?.let { score(it, targets.ofWhichSaturated)?.let(scores::add) }
        macros.ofWhichSugar?.let { score(it, targets.ofWhichSugar)?.let(scores::add) }
        macros.salt?.let { score(it, targets.salt)?.let(scores::add) }
        macros.fibre?.let { score(it, targets.fibre)?.let(scores::add) }

        return if (scores.isEmpty()) 0f else scores.average().toFloat()
    }

    private fun List<Record>.groupByWallClockDay(): List<TravelDay> {
        return this
            .sortedBy { it.timestamp.toInstant() }
            .groupByTo(
                destination = sortedMapOf(),
                keySelector = { it.timestamp.toLocalDate() },
                valueTransform = { it }
            )
            .map { (day, records) ->
                val start = records.minBy { it.timestamp.toInstant() }.timestamp
                val end = records.maxBy { it.timestamp.toInstant() }.timestamp
                TravelDay(
                    records = records.toList(),
                    day = day,
                    firstLog = start,
                    lastLog = end,
                )
            }
    }
}