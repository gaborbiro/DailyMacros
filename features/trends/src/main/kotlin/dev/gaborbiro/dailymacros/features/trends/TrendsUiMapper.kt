package dev.gaborbiro.dailymacros.features.trends

import androidx.compose.ui.graphics.Color
import dev.gaborbiro.dailymacros.features.shared.diaryDayStartTime
import dev.gaborbiro.dailymacros.features.shared.logicalDiaryDate
import dev.gaborbiro.dailymacros.features.shared.logicalDiaryToday
import dev.gaborbiro.dailymacros.features.trends.model.ChartDataPoint
import dev.gaborbiro.dailymacros.features.trends.model.ChartDataset
import dev.gaborbiro.dailymacros.features.trends.model.DayQualifier
import dev.gaborbiro.dailymacros.features.trends.model.TrendsChartUiModel
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

class TrendsUiMapper @Inject constructor(
    private val preferences: TrendsPreferences,
    private val settingsRepository: SettingsRepository,
) {

    private val diaryDayStart: LocalTime
        get() = diaryDayStartTime(settingsRepository.getDiaryDayStartHour())

    private fun Record.diaryKeyDate(): LocalDate =
        timestamp.logicalDiaryDate(diaryDayStart)

    private fun logicalToday(): LocalDate =
        logicalDiaryToday(ZoneId.systemDefault(), diaryDayStart)

    fun mapDaysCharts(
        records: List<Record>,
        dayQualifier: DayQualifier,
        targets: Targets,
    ): List<TrendsChartUiModel> {
        val recordsByPeriod = records.groupBy { it.diaryKeyDate() }

        val days: List<LocalDate> = generateSequence(
            from = recordsByPeriod.keys.minOrNull(),
            to = recordsByPeriod.keys.maxOrNull(),
            step = { it.plusDays(1) }
        )

        val weekFields = WeekFields.of(Locale.getDefault())
        val first = weekFields.firstDayOfWeek
        val last = first.minus(1)

        fun dayLabel(date: LocalDate): String {
            val base = if (date.dayOfMonth == 1)
                "${date.dayOfMonth}/${date.monthValue}"
            else
                "${date.dayOfMonth}"

            return when (date.dayOfWeek) {
                first -> "[$base"
                last -> "$base]"
                else -> base
            }
        }

        val today = logicalToday()

        return mapCharts(
            timeRange = days,
            records = recordsByPeriod,
            contributingDaysProvider = { day: LocalDate ->
                contributingDays(
                    records = recordsByPeriod[day].orEmpty(),
                    calendarDays = listOf(day),
                    dayQualifier = dayQualifier,
                )
            },
            currentPeriodCalculationMode = CurrentPeriodCalculationMode.Default(
                isCurrentPeriod = { it == today }
            ),
            labelProvider = ::dayLabel,
            targets = targets,
        )
    }

    fun mapWeeksCharts(
        records: List<Record>,
        dayQualifier: DayQualifier,
        targets: Targets,
    ): List<TrendsChartUiModel> {
        val weekFields = WeekFields.of(Locale.getDefault())

        val recordsByPeriod = records.groupBy { record ->
            record.diaryKeyDate().with(weekFields.dayOfWeek(), 1)
        }

        val weeks: List<LocalDate> = generateSequence(
            from = recordsByPeriod.keys.minOrNull(),
            to = recordsByPeriod.keys.maxOrNull(),
            step = { it.plusWeeks(1) }
        )

        fun weekLabel(start: LocalDate): String {
            val end = start.plusDays(6)
            val includesFirst = (0L..6).any { start.plusDays(it).dayOfMonth == 1 }

            return if (start.month == end.month && !includesFirst) {
                "${start.dayOfMonth}-${end.dayOfMonth}"
            } else {
                val month = end.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                "${start.dayOfMonth}-${end.dayOfMonth} $month"
            }
        }

        val today = logicalToday()

        val contributingDaysProvider: (LocalDate) -> List<LocalDate> = { weekStart ->
            val calendarDays: List<LocalDate> = (0L..6L).map { weekStart.plusDays(it) }
            contributingDays(
                records = recordsByPeriod[weekStart].orEmpty(),
                calendarDays = calendarDays,
                dayQualifier = dayQualifier,
            )
        }

        val currentPeriodMode = CurrentPeriodCalculationMode.Projected<LocalDate>(
            elapsedDaysProvider = { weekStart ->
                if (today in weekStart..weekStart.plusDays(6)) {
                    val elapsedCalendarDays: List<LocalDate> =
                        generateSequence(weekStart) { it.plusDays(1) }
                            .takeWhile { it <= today }
                            .toList()
                    contributingDays(
                        records = recordsByPeriod[weekStart].orEmpty(),
                        calendarDays = elapsedCalendarDays,
                        dayQualifier = dayQualifier,
                    )
                } else {
                    emptyList()
                }
            },
            minElapsedDays = 2,
            isCurrentPeriod = { start -> today in start..start.plusDays(6) },
        )

        return mapCharts(
            timeRange = weeks,
            records = recordsByPeriod,
            contributingDaysProvider = contributingDaysProvider,
            currentPeriodCalculationMode = currentPeriodMode,
            labelProvider = ::weekLabel,
            targets = targets,
        ) + listOf(
            adherenceChart(
                timeRange = weeks,
                records = recordsByPeriod,
                contributingDaysProvider = contributingDaysProvider,
                currentPeriodCalculationMode = currentPeriodMode,
                labelProvider = ::weekLabel,
                targets = targets,
            )
        )
    }

    fun mapMonthsCharts(
        records: List<Record>,
        aggregationMode: DayQualifier,
        targets: Targets,
    ): List<TrendsChartUiModel> {
        val recordsByPeriod = records.groupBy { YearMonth.from(it.diaryKeyDate()) }

        val months: List<YearMonth> = generateSequence(
            from = recordsByPeriod.keys.minOrNull(),
            to = recordsByPeriod.keys.maxOrNull(),
            step = { it.plusMonths(1) }
        )

        fun monthLabel(ym: YearMonth): String =
            ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

        val todayDate = logicalToday()
        val thisMonth = YearMonth.from(todayDate)

        val contributingDaysProvider: (YearMonth) -> List<LocalDate> = { ym ->
            val calendarDays: List<LocalDate> = (1..ym.lengthOfMonth()).map { day -> ym.atDay(day) }
            contributingDays(
                records = recordsByPeriod[ym].orEmpty(),
                calendarDays = calendarDays,
                dayQualifier = aggregationMode,
            )
        }

        val currentPeriodMode = CurrentPeriodCalculationMode.Projected<YearMonth>(
            elapsedDaysProvider = { ym ->
                if (ym == thisMonth) {
                    val elapsedCalendarDays: List<LocalDate> =
                        (1..todayDate.dayOfMonth).map { day -> ym.atDay(day) }
                    contributingDays(
                        records = recordsByPeriod[ym].orEmpty(),
                        calendarDays = elapsedCalendarDays,
                        dayQualifier = aggregationMode,
                    )
                } else {
                    emptyList()
                }
            },
            minElapsedDays = 7,
            isCurrentPeriod = { ym -> ym == thisMonth },
        )

        return mapCharts(
            timeRange = months,
            records = recordsByPeriod,
            contributingDaysProvider = contributingDaysProvider,
            currentPeriodCalculationMode = currentPeriodMode,
            labelProvider = ::monthLabel,
            targets = targets,
        ) + listOf(
            adherenceChart(
                timeRange = months,
                records = recordsByPeriod,
                contributingDaysProvider = contributingDaysProvider,
                currentPeriodCalculationMode = currentPeriodMode,
                labelProvider = ::monthLabel,
                targets = targets,
            )
        )
    }

    private fun <K : Comparable<K>> mapCharts(
        timeRange: List<K>,
        records: Map<K, List<Record>>,
        contributingDaysProvider: (K) -> List<LocalDate>,
        currentPeriodCalculationMode: CurrentPeriodCalculationMode<K>,
        labelProvider: (K) -> String,
        targets: Targets,
    ): List<TrendsChartUiModel> {

        fun agg(valueProvider: (Record) -> Float?) =
            computePeriodAverages(
                timeRange = timeRange,
                records = records,
                contributingDaysProvider = contributingDaysProvider,
                currentPeriodCalculationMode = currentPeriodCalculationMode,
                valueProvider = valueProvider,
                labelProvider = labelProvider,
            )

        val noTarget = Target(enabled = false)

        return datasetsOf(
            "Calories" to listOf(
                series("Calories (kcal)", Color(0xFF8AB4F8), agg { it.template.nutrients.calories?.toFloat() }, targets.calories),
            ),
            "Protein" to listOf(
                series("Protein (g)", Color(0xFF81C995), agg { it.template.nutrients.protein }, targets.protein),
            ),
            "Carbs" to listOf(
                series("Carbs (g)", Color(0xFFFFC278), agg { it.template.nutrients.carbs }, targets.carbs),
                series("  └>of which sugar (g)", Color(0xFFFFB74D), agg { it.template.nutrients.ofWhichSugar }, targets.ofWhichSugar),
                series("      └>of which added sugar (g)", Color(0xFFFF802C), agg { it.template.nutrients.ofWhichAddedSugar }, noTarget),
            ),
            "Fat" to listOf(
                series("Fat (g)", Color(0xFFFFA6A6), agg { it.template.nutrients.fat }, targets.fat),
                series(" └>of which saturated fat (g)", Color(0xFFE57373), agg { it.template.nutrients.ofWhichSaturated }, targets.ofWhichSaturated),
            ),
            "Salt" to listOf(
                series("Salt (g)", Color(0xFFB39DDB), agg { it.template.nutrients.salt }, targets.salt),
            ),
            "Fibre" to listOf(
                series("Fibre (g)", Color(0xFF4DB6AC), agg { it.template.nutrients.fibre }, targets.fibre),
            ),
        )
    }

    private fun series(
        name: String,
        color: Color,
        points: Pair<List<ChartDataPoint>, ChartDataPoint?>,
        target: Target,
    ): SeriesWithTarget = SeriesWithTarget(name, color, points, target)

    private data class SeriesWithTarget(
        val name: String,
        val color: Color,
        val data: Pair<List<ChartDataPoint>, ChartDataPoint?>,
        val target: Target,
    )

    private fun targetHorizontalValues(target: Target): Pair<Double?, Double?> {
        if (!target.enabled) return null to null
        return target.min?.toDouble() to target.max?.toDouble()
    }

    private fun contributingDays(
        records: List<Record>,
        calendarDays: List<LocalDate>,
        dayQualifier: DayQualifier,
    ): List<LocalDate> {

        return when (dayQualifier) {
            DayQualifier.ALL_CALENDAR_DAYS ->
                calendarDays

            DayQualifier.ONLY_LOGGED_DAYS ->
                records
                    .map { it.diaryKeyDate() }
                    .distinct()

            DayQualifier.ONLY_QUALIFIED_DAYS -> {
                val calorieTotals = calorieTotalsByDay(records)

                calorieTotals
                    .filterValues { it >= preferences.qualifyingCalorieThreshold }
                    .keys
                    .toList()
            }
        }
    }

    private fun calorieTotalsByDay(records: List<Record>): Map<LocalDate, Float> =
        records
            .mapNotNull { record ->
                record.template.nutrients.calories?.toFloat()?.let {
                    record.diaryKeyDate() to it
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }

    private fun <K> computePeriodAverages(
        timeRange: List<K>,
        records: Map<K, List<Record>>,
        contributingDaysProvider: (K) -> List<LocalDate>,
        currentPeriodCalculationMode: CurrentPeriodCalculationMode<K>,
        valueProvider: (Record) -> Float?,
        labelProvider: (K) -> String,
    ): Pair<List<ChartDataPoint>, ChartDataPoint?> {
        val points: List<Pair<K, ChartDataPoint>> = timeRange.mapIndexed { index, key ->
            val dailySums: Map<LocalDate, Float> =
                records[key]
                    ?.mapNotNull { record ->
                        valueProvider(record)?.let { value ->
                            record.diaryKeyDate() to value
                        }
                    }
                    ?.groupBy({ it.first }, { it.second })
                    ?.mapValues { (_, values) -> values.sum() }
                    ?: emptyMap()

            val contributingDays: List<LocalDate> = when {
                currentPeriodCalculationMode.isCurrentPeriod(key) && currentPeriodCalculationMode is CurrentPeriodCalculationMode.Projected ->
                    currentPeriodCalculationMode.elapsedDaysProvider(key)

                else ->
                    contributingDaysProvider(key)
            }

            val contributingValues: List<Float> =
                contributingDays.map { day -> dailySums[day] ?: 0f }

            val avg: Double? = when {
                currentPeriodCalculationMode.isCurrentPeriod(key) && currentPeriodCalculationMode is CurrentPeriodCalculationMode.Hidden ->
                    null

                currentPeriodCalculationMode.isCurrentPeriod(key) && currentPeriodCalculationMode is CurrentPeriodCalculationMode.Projected ->
                    contributingValues
                        .takeIf { it.size >= currentPeriodCalculationMode.minElapsedDays }
                        ?.average()

                else ->
                    contributingValues
                        .takeIf { it.isNotEmpty() }
                        ?.average()
            }

            key to ChartDataPoint(
                index = index,
                label = labelProvider(key),
                value = avg,
            )
        }

        val last = points.lastOrNull() ?: return emptyList<ChartDataPoint>() to null

        return if (currentPeriodCalculationMode.isCurrentPeriod(last.first)) {
            points.dropLast(1).map { it.second } to last.second
        } else {
            points.map { it.second } to null
        }
    }

    private fun datasetsOf(
        vararg rows: Pair<String, List<SeriesWithTarget>>,
    ): List<TrendsChartUiModel> {
        return rows.map { (title, chartConfig) ->
            TrendsChartUiModel(
                title = title,
                datasets = chartConfig.map { spec ->
                    val (set: List<ChartDataPoint>, current) = spec.data
                    val (minY, maxY) = targetHorizontalValues(spec.target)
                    ChartDataset(
                        name = spec.name,
                        color = spec.color,
                        set = set,
                        current = current,
                        targetMinY = minY,
                        targetMaxY = maxY,
                    )
                },
            )
        }
    }

    private sealed class CurrentPeriodCalculationMode<K>(
        open val isCurrentPeriod: (K) -> Boolean,
    ) {
        data class Hidden<K>(
            override val isCurrentPeriod: (K) -> Boolean,
        ) : CurrentPeriodCalculationMode<K>(isCurrentPeriod)

        data class Default<K>(
            override val isCurrentPeriod: (K) -> Boolean,
        ) : CurrentPeriodCalculationMode<K>(isCurrentPeriod)

        data class Projected<K>(
            val elapsedDaysProvider: (@UnsafeVariance K) -> List<LocalDate>,
            val minElapsedDays: Int = 2,
            override val isCurrentPeriod: (K) -> Boolean,
        ) : CurrentPeriodCalculationMode<K>(isCurrentPeriod)
    }

    private fun <K : Comparable<K>> adherenceChart(
        timeRange: List<K>,
        records: Map<K, List<Record>>,
        contributingDaysProvider: (K) -> List<LocalDate>,
        currentPeriodCalculationMode: CurrentPeriodCalculationMode<K>,
        labelProvider: (K) -> String,
        targets: Targets,
    ): TrendsChartUiModel {
        fun adherenceForDays(key: K, days: List<LocalDate>): Double? {
            if (days.isEmpty()) return null
            val byDay = records[key].orEmpty().groupBy { it.diaryKeyDate() }
            return days.map { day ->
                calculateAdherence(byDay[day].orEmpty().sumNutrients(), targets).toDouble()
            }.average()
        }

        val points = timeRange.mapIndexed { index, key ->
            val contributingDays = when {
                currentPeriodCalculationMode.isCurrentPeriod(key) && currentPeriodCalculationMode is CurrentPeriodCalculationMode.Projected ->
                    currentPeriodCalculationMode.elapsedDaysProvider(key)
                else ->
                    contributingDaysProvider(key)
            }

            val avg = when {
                currentPeriodCalculationMode.isCurrentPeriod(key) && currentPeriodCalculationMode is CurrentPeriodCalculationMode.Hidden ->
                    null
                currentPeriodCalculationMode.isCurrentPeriod(key) && currentPeriodCalculationMode is CurrentPeriodCalculationMode.Projected ->
                    if (contributingDays.size >= currentPeriodCalculationMode.minElapsedDays)
                        adherenceForDays(key, contributingDays)
                    else
                        null
                else ->
                    adherenceForDays(key, contributingDays)
            }

            key to ChartDataPoint(
                index = index,
                label = labelProvider(key),
                value = avg?.times(100.0),
            )
        }

        val last = points.lastOrNull()
        val (historicalPoints, currentPoint) = if (last != null && currentPeriodCalculationMode.isCurrentPeriod(last.first)) {
            points.dropLast(1).map { it.second } to last.second
        } else {
            points.map { it.second } to null
        }

        return TrendsChartUiModel(
            title = "Adherence",
            datasets = listOf(
                ChartDataset(
                    name = "Adherence (%)",
                    color = Color(0xFF66BB6A),
                    set = historicalPoints,
                    current = currentPoint,
                )
            ),
            pinnedMaxY = 100.0,
        )
    }

    private fun calculateAdherence(nutrients: TemplateNutrientBreakdown, targets: Targets): Float {
        val scores = mutableListOf<Float>()

        fun score(value: Float?, target: Target): Float? {
            if (!target.enabled || value == null) return null
            val min = target.min?.toFloat()
            val max = target.max?.toFloat()
            if (min == null && max == null) return null
            if ((min == null || value >= min) && (max == null || value <= max)) return 1f
            if (min != null && value < min) {
                return (1f - ((min - value) / min.coerceAtLeast(1f))).coerceIn(0f, 1f)
            }
            if (max != null && value > max) {
                return (1f - ((value - max) / max.coerceAtLeast(1f))).coerceIn(0f, 1f)
            }
            return null
        }

        nutrients.calories?.toFloat()?.let { score(it, targets.calories)?.let(scores::add) }
        nutrients.protein?.let { score(it, targets.protein)?.let(scores::add) }
        nutrients.fat?.let { score(it, targets.fat)?.let(scores::add) }
        nutrients.carbs?.let { score(it, targets.carbs)?.let(scores::add) }
        nutrients.ofWhichSaturated?.let { score(it, targets.ofWhichSaturated)?.let(scores::add) }
        nutrients.ofWhichSugar?.let { score(it, targets.ofWhichSugar)?.let(scores::add) }
        nutrients.salt?.let { score(it, targets.salt)?.let(scores::add) }
        nutrients.fibre?.let { score(it, targets.fibre)?.let(scores::add) }

        return if (scores.isEmpty()) 0f else scores.average().toFloat()
    }

    private fun List<Record>.sumNutrients(): TemplateNutrientBreakdown = TemplateNutrientBreakdown(
        calories = mapNotNull { it.template.nutrients.calories }.takeIf { it.isNotEmpty() }?.sum(),
        protein = mapNotNull { it.template.nutrients.protein }.takeIf { it.isNotEmpty() }?.sum(),
        fat = mapNotNull { it.template.nutrients.fat }.takeIf { it.isNotEmpty() }?.sum(),
        ofWhichSaturated = mapNotNull { it.template.nutrients.ofWhichSaturated }.takeIf { it.isNotEmpty() }?.sum(),
        carbs = mapNotNull { it.template.nutrients.carbs }.takeIf { it.isNotEmpty() }?.sum(),
        ofWhichSugar = mapNotNull { it.template.nutrients.ofWhichSugar }.takeIf { it.isNotEmpty() }?.sum(),
        ofWhichAddedSugar = mapNotNull { it.template.nutrients.ofWhichAddedSugar }.takeIf { it.isNotEmpty() }?.sum(),
        salt = mapNotNull { it.template.nutrients.salt }.takeIf { it.isNotEmpty() }?.sum(),
        fibre = mapNotNull { it.template.nutrients.fibre }.takeIf { it.isNotEmpty() }?.sum(),
    )

    private fun <T : Comparable<T>> generateSequence(
        from: T?,
        to: T?,
        step: (T) -> T,
    ): List<T> {
        if (from == null || to == null) return emptyList()

        return generateSequence(from) { prev ->
            val next = step(prev)
            if (next <= to) next else null
        }.toList()
    }

    fun map(mode: DayQualifier): String {
        return when (mode) {
            DayQualifier.ALL_CALENDAR_DAYS -> TrendsPreferences.MODE_ALL_CALENDAR_DAYS
            DayQualifier.ONLY_LOGGED_DAYS -> TrendsPreferences.MODE_ONLY_LOGGED_DAYS
            DayQualifier.ONLY_QUALIFIED_DAYS -> TrendsPreferences.MODE_ONLY_QUALIFIED_DAYS
        }
    }

    fun map(mode: String): DayQualifier {
        return when (mode) {
            TrendsPreferences.MODE_ALL_CALENDAR_DAYS -> DayQualifier.ALL_CALENDAR_DAYS
            TrendsPreferences.MODE_ONLY_LOGGED_DAYS -> DayQualifier.ONLY_LOGGED_DAYS
            TrendsPreferences.MODE_ONLY_QUALIFIED_DAYS -> DayQualifier.ONLY_QUALIFIED_DAYS
            else -> error("Unexpected day qualification mode: $mode")
        }
    }
}
