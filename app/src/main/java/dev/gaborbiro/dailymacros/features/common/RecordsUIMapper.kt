package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.features.common.model.ListUIModelBase
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelRecord
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelWeeklyReport
import dev.gaborbiro.dailymacros.repo.records.domain.model.Macros
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.settings.model.Target
import dev.gaborbiro.dailymacros.repo.settings.model.Targets
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

internal class RecordsUIMapper(
    private val macrosUIMapper: MacrosUIMapper,
    private val dateUIMapper: DateUIMapper,
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
            result += travelDay.records.map { map(it, showDay) }
            result += macrosUIMapper.mapMacroProgressTable(travelDay, targets)

            val lookAhead = grouped.getOrNull(index + 1)
            val weekEnded =
                // Case 1: current record is end of its calendar week
                travelDay.day.dayOfWeek == firstDayOfWeek.minus(1) ||
                        // Case 2: next record is in a *different* week
                        (lookAhead != null && lookAhead.day.get(weekFields.weekOfWeekBasedYear()) != travelDay.day.get(weekFields.weekOfWeekBasedYear())) ||
                        // Case 3: this is the last record, and it’s from a *past* week
                        (lookAhead == null && travelDay.day.isBefore(today.with(firstDayOfWeek)))

            if (weekEnded) {
                mapWeeklySummary(currentWeek, targets)?.let { result += it }
                currentWeek.clear()
            }
        }

        return result.reversed()
    }

    fun map(
        records: List<Record>,
        showDay: Boolean,
    ): List<ListUIModelBase> {
        return records.map {
            map(it, showDay)
        }
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

        val hours = dailyTotals.sumOf { it.duration.toHours() }
        println(hours)
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
            weeklyProgress = macrosUIMapper.buildMacroProgressItems(avgMacros, targets),
            averageAdherence100Percentage = (avgAdherence * 100).roundToInt()
        )
    }

    fun calculateAdherence(
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

    private fun map(record: Record, forceDay: Boolean): ListUIModelRecord {
        val timestampStr = dateUIMapper.mapRecordTimestamp(record.timestamp, forceDay)

        val macros = record.template.macros
            ?.let { macrosUIMapper.mapMacros(it) }

        return ListUIModelRecord(
            recordId = record.recordId,
            templateId = record.template.dbId,
            images = record.template.images,
            timestamp = timestampStr,
            title = record.template.name,
            macros = macros,
            showLoadingIndicator = record.template.isPending,
        )
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
