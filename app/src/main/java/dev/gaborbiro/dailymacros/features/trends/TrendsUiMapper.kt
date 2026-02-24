package dev.gaborbiro.dailymacros.features.trends

import androidx.compose.ui.graphics.Color
import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.trends.model.ChartDataPoint
import dev.gaborbiro.dailymacros.features.trends.model.ChartDataset
import dev.gaborbiro.dailymacros.features.trends.model.DayQualifier
import dev.gaborbiro.dailymacros.features.trends.model.TrendsChartUiModel
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

internal class TrendsUiMapper(
    private val appPrefs: AppPrefs,
) {

    fun mapDaysCharts(
        records: List<Record>,
        dayQualifier: DayQualifier,
    ): List<TrendsChartUiModel> {
        val recordsByPeriod = records.groupBy { it.timestamp.toLocalDate() }

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

        val today = LocalDate.now()

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
        )
    }

    fun mapWeeksCharts(
        records: List<Record>,
        dayQualifier: DayQualifier,
    ): List<TrendsChartUiModel> {
        val weekFields = WeekFields.of(Locale.getDefault())

        val recordsByPeriod = records.groupBy { record ->
            record.timestamp.toLocalDate().with(weekFields.dayOfWeek(), 1)
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

        val today = LocalDate.now()

        return mapCharts(
            timeRange = weeks,
            records = recordsByPeriod,
            contributingDaysProvider = { weekStart: LocalDate ->
                val calendarDays: List<LocalDate> =
                    (0L..6L)
                        .map { weekStart.plusDays(it) }

                contributingDays(
                    records = recordsByPeriod[weekStart].orEmpty(),
                    calendarDays = calendarDays,
                    dayQualifier = dayQualifier,
                )
            },
            currentPeriodCalculationMode = CurrentPeriodCalculationMode.Projected(
                elapsedDaysProvider = { weekStart: LocalDate ->
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
                isCurrentPeriod = { start ->
                    today in start..start.plusDays(6)
                },
            ),
            labelProvider = ::weekLabel,
        )
    }

    fun mapMonthsCharts(
        records: List<Record>,
        aggregationMode: DayQualifier,
    ): List<TrendsChartUiModel> {
        val recordsByPeriod = records.groupBy { YearMonth.from(it.timestamp.toLocalDate()) }

        val months: List<YearMonth> = generateSequence(
            from = recordsByPeriod.keys.minOrNull(),
            to = recordsByPeriod.keys.maxOrNull(),
            step = { it.plusMonths(1) }
        )

        fun monthLabel(ym: YearMonth): String =
            ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

        val thisMonth = YearMonth.now()
        val todayDate = LocalDate.now()

        return mapCharts(
            timeRange = months,
            records = recordsByPeriod,
            contributingDaysProvider = { ym: YearMonth ->
                val calendarDays: List<LocalDate> =
                    (1..ym.lengthOfMonth())
                        .map { day -> ym.atDay(day) }

                contributingDays(
                    records = recordsByPeriod[ym].orEmpty(),
                    calendarDays = calendarDays,
                    dayQualifier = aggregationMode,
                )
            },
            currentPeriodCalculationMode = CurrentPeriodCalculationMode.Projected(
                elapsedDaysProvider = { ym: YearMonth ->
                    if (ym == thisMonth) {
                        val elapsedCalendarDays: List<LocalDate> =
                            (1..todayDate.dayOfMonth)
                                .map { day -> ym.atDay(day) }

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
                isCurrentPeriod = { ym ->
                    ym == thisMonth
                },
            ),
            labelProvider = ::monthLabel,
        )
    }

    private fun <K : Comparable<K>> mapCharts(
        timeRange: List<K>,
        records: Map<K, List<Record>>,
        contributingDaysProvider: (K) -> List<LocalDate>,
        currentPeriodCalculationMode: CurrentPeriodCalculationMode<K>,
        labelProvider: (K) -> String,
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

        return datasetsOf(
            listOf(
                Triple("Calories (kcal)", Color(0xFF8AB4F8), agg { it.template.nutrientBreakdown.calories?.toFloat() })
            ),
            listOf(
                Triple("Protein (g)", Color(0xFF81C995), agg { it.template.nutrientBreakdown.protein })
            ),
            listOf(
                Triple("Carbs (g)", Color(0xFFFFC278), agg { it.template.nutrientBreakdown.carbs }),
                Triple("  └>of which sugar (g)", Color(0xFFFFB74D), agg { it.template.nutrientBreakdown.ofWhichSugar }),
                Triple("      └>of which added sugar (g)", Color(0xFFFF802C), agg { it.template.nutrientBreakdown.ofWhichAddedSugar })
            ),
            listOf(
                Triple("Fat (g)", Color(0xFFFFA6A6), agg { it.template.nutrientBreakdown.fat }),
                Triple(" └>of which saturated fat (g)", Color(0xFFE57373), agg { it.template.nutrientBreakdown.ofWhichSaturated })
            ),
            listOf(
                Triple("Salt (g)", Color(0xFFB39DDB), agg { it.template.nutrientBreakdown.salt })
            ),
            listOf(
                Triple("Fibre (g)", Color(0xFF4DB6AC), agg { it.template.nutrientBreakdown.fibre })
            ),
        )
    }

    /**
     * Returns the days within a period that should count toward the average (i.e. the denominator).
     *
     * Which days "count" depends on [dayQualifier]:
     * - [DayQualifier.ALL_CALENDAR_DAYS]: every calendar day in the period.
     * - [DayQualifier.ONLY_LOGGED_DAYS]: only days that have at least one record.
     * - [DayQualifier.ONLY_QUALIFIED_DAYS]: only days whose total calories meet [AppPrefs.qualifyingCalorieThreshold]
     *
     * @param records records from the given [calendarDays].
     * @param calendarDays the full set of calendar days to be considered.
     * @param dayQualifier which filtering strategy to apply.
     * @return the subset of days that contribute to the period's average.
     */
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
                    .map { it.timestamp.toLocalDate() }
                    .distinct()

            DayQualifier.ONLY_QUALIFIED_DAYS -> {
                val calorieTotals = calorieTotalsByDay(records)

                calorieTotals
                    .filterValues { it >= appPrefs.qualifyingCalorieThreshold }
                    .keys
                    .toList()
            }
        }
    }

    private fun calorieTotalsByDay(records: List<Record>): Map<LocalDate, Float> =
        records
            .mapNotNull { record ->
                record.template.nutrientBreakdown.calories?.toFloat()?.let {
                    record.timestamp.toLocalDate() to it
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }

    /**
     * Computes the average daily nutrient intake per time period, producing chart-ready data points.
     *
     * Completed periods:
     * - Average is computed over days returned by [contributingDaysProvider]
     *   (days with no or below threshold records contribute 0).
     *
     * Current (in-progress) period behavior depends on [currentPeriodCalculationMode]:
     * - [CurrentPeriodCalculationMode.Hidden]: the current period is excluded (returned as `null`).
     * - [CurrentPeriodCalculationMode.Default]: averaged the same way as completed periods.
     * - [CurrentPeriodCalculationMode.Projected]: averaged over elapsed days only
     *   (returned by [CurrentPeriodCalculationMode.Projected.elapsedDaysProvider]).
     *   Requires at least [CurrentPeriodCalculationMode.Projected.minElapsedDays] days; otherwise `null`.
     *   This prevents the current period being artificially deflated by future days being treated as 0.
     *
     * @param K the type of period key (e.g. [YearMonth], [LocalDate], or an iso-week pair).
     * @param timeRange ordered list of time period keys defining the x-axis of the chart.
     * @param records records pre-grouped by the same period keys.
     * @param contributingDaysProvider days that contribute to completed-period averages (denominator).
     * @param currentPeriodCalculationMode controls how the current period is handled. See [CurrentPeriodCalculationMode].
     * @param valueProvider extracts the nutrient value from a [Record], or `null` if absent.
     * @param labelProvider produces the human-readable x-axis label for a period.
     * @return a pair of (completed data points, current-period data point or `null`).
     */
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
                            record.timestamp.toLocalDate() to value
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
        vararg tuples: List<Triple<String, Color, Pair<List<ChartDataPoint>, ChartDataPoint?>>>
    ): List<TrendsChartUiModel> {
        return tuples.map { chartConfig ->
            TrendsChartUiModel(
                chartConfig.map { (name, color, points) ->
                    val (set: List<ChartDataPoint>, current) = points
                    ChartDataset(
                        name = name,
                        color = color,
                        set = set,
                        current = current,
                    )
                }
            )
        }
    }

    private sealed class CurrentPeriodCalculationMode<K>(
        open val isCurrentPeriod: (K) -> Boolean,
    ) {
        /** Don't show the current period at all. */
        data class Hidden<K>(
            override val isCurrentPeriod: (K) -> Boolean,
        ) : CurrentPeriodCalculationMode<K>(isCurrentPeriod)

        /** Show the raw average so far, same calculation as completed periods. */
        data class Default<K>(
            override val isCurrentPeriod: (K) -> Boolean,
        ) : CurrentPeriodCalculationMode<K>(isCurrentPeriod)

        /** Extrapolate from elapsed days to project a full-period average. */
        data class Projected<K>(
            val elapsedDaysProvider: (@UnsafeVariance K) -> List<LocalDate>,
            val minElapsedDays: Int = 2,
            override val isCurrentPeriod: (K) -> Boolean,
        ) : CurrentPeriodCalculationMode<K>(isCurrentPeriod)
    }

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
            DayQualifier.ALL_CALENDAR_DAYS -> AppPrefs.DAY_QUALIFICATION_MODE_ALL_CALENDAR_DAYS
            DayQualifier.ONLY_LOGGED_DAYS -> AppPrefs.DAY_QUALIFICATION_MODE_ONLY_LOGGED_DAYS
            DayQualifier.ONLY_QUALIFIED_DAYS -> AppPrefs.DAY_QUALIFICATION_MODE_ONLY_QUALIFIED_DAYS
        }
    }

    fun map(@AppPrefs.Companion.DayQualificationMode mode: String): DayQualifier {
        return when (mode) {
            AppPrefs.DAY_QUALIFICATION_MODE_ALL_CALENDAR_DAYS -> DayQualifier.ALL_CALENDAR_DAYS
            AppPrefs.DAY_QUALIFICATION_MODE_ONLY_LOGGED_DAYS -> DayQualifier.ONLY_LOGGED_DAYS
            AppPrefs.DAY_QUALIFICATION_MODE_ONLY_QUALIFIED_DAYS -> DayQualifier.ONLY_QUALIFIED_DAYS
            else -> error("Unexpected day qualification mode: $mode")
        }
    }
}
