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
        var previousWeek: List<TravelDay>? = null
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
                mapWeeklySummary(
                    week = currentWeek,
                    previousWeek = previousWeek,
                    targets = targets,
                )?.let { result += it }
                previousWeek = currentWeek.toList()
                currentWeek.clear()
            }
        }

        return result.reversed()
    }

    private fun mapWeeklySummary(
        week: List<TravelDay>,
        previousWeek: List<TravelDay>?,
        targets: Targets,
    ): ListUIModelWeeklyReport? {
        if (week.isEmpty()) return null

        // 1. Compute total macros per day for a given week
        data class DayTotal(
            val macros: Macros,
            val duration: Duration,
        )

        fun computeDailyTotals(days: List<TravelDay>): List<DayTotal> {
            return days.mapNotNull { day ->
                val dayMacros = day.records.mapNotNull { it.template.macros }
                if (dayMacros.isEmpty()) return@mapNotNull null

                val sum = dayMacros.reduce { acc, m ->
                    Macros(
                        calories = (acc.calories ?: 0) + (m.calories ?: 0),
                        protein = (acc.protein ?: 0f) + (m.protein ?: 0f),
                        fat = (acc.fat ?: 0f) + (m.fat ?: 0f),
                        ofWhichSaturated = (acc.ofWhichSaturated ?: 0f) + (m.ofWhichSaturated ?: 0f),
                        carbs = (acc.carbs ?: 0f) + (m.carbs ?: 0f),
                        ofWhichSugar = (acc.ofWhichSugar ?: 0f) + (m.ofWhichSugar ?: 0f),
                        ofWhichAddedSugar = (acc.ofWhichAddedSugar ?: 0f) + (m.ofWhichAddedSugar ?: 0f),
                        salt = (acc.salt ?: 0f) + (m.salt ?: 0f),
                        fibre = (acc.fibre ?: 0f) + (m.fibre ?: 0f),
                        notes = null,
                    )
                }

                DayTotal(sum, day.duration)
            }
        }

        val dailyTotals = computeDailyTotals(week)

        if (dailyTotals.isEmpty()) return null

        // 2. Weighted average helper (by TravelDay duration — long travel days contribute more)
        fun weightedAvg(dailyTotals: List<DayTotal>, selector: (Macros) -> Number?): Number? {
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

        fun computeWeeklyAverages(dailyTotals: List<DayTotal>): Macros {
            return Macros(
                calories = weightedAvg(dailyTotals) { it.calories }?.toInt(),
                protein = weightedAvg(dailyTotals) { it.protein }?.toFloat(),
                fat = weightedAvg(dailyTotals) { it.fat }?.toFloat(),
                ofWhichSaturated = weightedAvg(dailyTotals) { it.ofWhichSaturated }?.toFloat(),
                carbs = weightedAvg(dailyTotals) { it.carbs }?.toFloat(),
                ofWhichSugar = weightedAvg(dailyTotals) { it.ofWhichSugar }?.toFloat(),
                ofWhichAddedSugar = weightedAvg(dailyTotals) { it.ofWhichAddedSugar }?.toFloat(),
                salt = weightedAvg(dailyTotals) { it.salt }?.toFloat(),
                fibre = weightedAvg(dailyTotals) { it.fibre }?.toFloat(),
                notes = null,
            )
        }

        // 3. Compute weekly weighted averages for current week
        val avgMacros = computeWeeklyAverages(dailyTotals)

        // 4. Compute previous week's weighted averages if available
        val previousWeekMacros = previousWeek
            ?.let { prev -> computeDailyTotals(prev) }
            ?.takeIf { it.isNotEmpty() }
            ?.let { computeWeeklyAverages(it) }

        // 5. Compare weekly averages against daily targets (no scaling) for adherence
        val avgAdherence = calculateAdherence(
            macros = avgMacros,
            targets = targets,
            duration = Duration.ofHours(24) // baseline: daily target
        )

        val prevWeekAvgAdherence = previousWeekMacros?.let {
            calculateAdherence(
                macros = previousWeekMacros,
                targets = targets,
                duration = Duration.ofHours(24) // baseline: daily target
            )
        }

        val weekStart = week.minOf { it.day }

        return ListUIModelWeeklyReport(
            listItemId = weekStart.toEpochDay(),
            weeklyProgress = buildWeeklySummaryMacroItems(avgMacros, targets, previousWeekMacros),
            averageAdherence100Percentage = (avgAdherence * 100).roundToInt(),
            adherenceChange = calculateChangeIndicator(avgAdherence, prevWeekAvgAdherence),
        )
    }

    /**
     * Builds weekly summary macro items with change indicators.
     * Change is calculated relative to previous week (showing week-over-week change).
     */
    private fun buildWeeklySummaryMacroItems(
        macros: Macros,
        targets: Targets,
        previousWeekMacros: Macros? = null,
    ): List<WeeklySummaryMacroProgressItem> {
        return buildList {
            targets.calories.takeIf { it.enabled }?.let {
                add(
                    WeeklySummaryMacroProgressItem(
                        title = "Calories",
                        progress0to1 = macrosUIMapper.targetProgress(it, macros.calories?.toFloat() ?: 0f) ?: 0f,
                        progressLabel = macrosUIMapper.formatCalories(macros.calories, isShort = false, withLabel = false) ?: "0",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.calories?.toFloat(),
                            previous = previousWeekMacros?.calories?.toFloat(),
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
                        progressLabel = macrosUIMapper.formatProtein(macros.protein, isShort = false, withLabel = false) ?: "0g",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.protein,
                            previous = previousWeekMacros?.protein,
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
                        progressLabel = macrosUIMapper.formatSalt(macros.salt, isShort = false, withLabel = false) ?: "0.0g",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.salt,
                            previous = previousWeekMacros?.salt,
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
                        progressLabel = macrosUIMapper.formatFibre(macros.fibre, isShort = false, withLabel = false) ?: "0g",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.fibre,
                            previous = previousWeekMacros?.fibre,
                        ),
                        color = { it.fibreColor },
                    )
                )
            }
            targets.carbs.takeIf { it.enabled }?.let {
                add(
                    WeeklySummaryMacroProgressItem(
                        title = "Carbs",
                        progress0to1 = macrosUIMapper.targetProgress(it, macros.carbs ?: 0f) ?: 0f,
                        progressLabel = macrosUIMapper.formatCarbs(macros.carbs, sugar = null, addedSugar = null, isShort = false, withLabel = false)
                            ?: "0g",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.carbs,
                            previous = previousWeekMacros?.carbs,
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
                        progressLabel = macrosUIMapper.formatSugar(macros.ofWhichSugar, isShort = false, withLabel = false) ?: "0g",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.ofWhichSugar,
                            previous = previousWeekMacros?.ofWhichSugar,
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
                        progressLabel = macrosUIMapper.formatFat(macros.fat, saturated = null, isShort = false, withLabel = false)
                            ?: "0g",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.fat,
                            previous = previousWeekMacros?.fat,
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
                        progressLabel = macrosUIMapper.formatSaturatedFat(macros.ofWhichSaturated, isShort = false, withLabel = false) ?: "0g",
                        targetRange0to1 = macrosUIMapper.targetRange(it),
                        changeIndicator = calculateChangeIndicator(
                            current = macros.ofWhichSaturated,
                            previous = previousWeekMacros?.ofWhichSaturated,
                        ),
                        color = { it.fatColor },
                    )
                )
            }
        }
    }

    /**
     * Calculates change indicator based on week-over-week comparison.
     * Compares current week macros to previous week macros.
     */
    private fun calculateChangeIndicator(
        current: Float?,
        previous: Float?,
    ): ChangeIndicator {
        if (current == null || current == 0f) {
            return ChangeIndicator(ChangeDirection.NEUTRAL, "")
        }

        if (previous == null || previous == 0f) {
            return ChangeIndicator(ChangeDirection.NEUTRAL, "")
        }

        val deviation = current - previous
        val percentChange = deviation / previous

        val direction = when {
            percentChange * 100 > 2f -> ChangeDirection.UP
            percentChange * 100 < -2f -> ChangeDirection.DOWN
            else -> ChangeDirection.NEUTRAL
        }

        val formattedValue = DecimalFormat("+#%;-#%").format(percentChange)
        return ChangeIndicator(direction, formattedValue)
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
        macros.carbs?.let { score(it, targets.carbs)?.let(scores::add) }
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