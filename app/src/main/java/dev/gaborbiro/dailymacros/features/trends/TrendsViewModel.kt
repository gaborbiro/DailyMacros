package dev.gaborbiro.dailymacros.features.trends

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.trends.model.ChartDataPoint
import dev.gaborbiro.dailymacros.features.trends.model.ChartDataset
import dev.gaborbiro.dailymacros.features.trends.model.DailyAggregationMode
import dev.gaborbiro.dailymacros.features.trends.model.Timescale
import dev.gaborbiro.dailymacros.features.trends.model.TrendsChartUiModel
import dev.gaborbiro.dailymacros.features.trends.model.TrendsSettingsUIModel
import dev.gaborbiro.dailymacros.features.trends.model.TrendsViewState
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale


internal class TrendsViewModel(
    private val navigator: TrendsNavigator,
    private val recordsRepository: RecordsRepository,
    private val appPrefs: AppPrefs,
    private val mapper: TrendsMapper,
) : ViewModel() {

    private var recordsJob: Job

    private val _viewState: MutableStateFlow<TrendsViewState> =
        MutableStateFlow(
            TrendsViewState()
        )
    val viewState: StateFlow<TrendsViewState> = _viewState.asStateFlow()

    init {
        recordsJob = observeRecords(Timescale.DAYS)
    }

    fun onTimescaleSelected(timescale: Timescale) {
        recordsJob.cancel()
        recordsJob = observeRecords(timescale)
    }

    fun onBackNavigate() {
        navigator.navigateBack()
    }

    fun onSettingsActionButtonClicked() {
        _viewState.value = _viewState.value.copy(
            settings = TrendsSettingsUIModel.Show(
                dailyAggregationMode = mapper.map(appPrefs.aggregationMode),
                qualifiedDaysThreshold = appPrefs.qualifiedAggregationThreshold,
            )
        )
    }

    fun onSettingsCloseRequested() {
        _viewState.value = _viewState.value.copy(settings = TrendsSettingsUIModel.Hidden)
    }

    fun onAggregationModeChanged(dailyAggregationMode: DailyAggregationMode, timescale: Timescale) {
        appPrefs.aggregationMode = mapper.map(dailyAggregationMode)
        recordsJob.cancel()
        recordsJob = observeRecords(timescale)
    }

    fun onAggregationThresholdChanged(threshold: Long, timescale: Timescale) {
        appPrefs.qualifiedAggregationThreshold = threshold
        recordsJob.cancel()
        recordsJob = observeRecords(timescale)
    }

    private fun observeRecords(timescale: Timescale): Job {
        // Re-launch collection each timescale changes
        return viewModelScope.launch {
            // Use the same flow as overview (no search term = all records)
            recordsRepository.getFlowBySearchTerm(null).collect { records ->
                if (records.isEmpty()) {
                    _viewState.value = _viewState.value.copy(charts = emptyList())
                } else {
                    val aggregationMode = mapper.map(appPrefs.aggregationMode)
                    val charts = when (timescale) {
                        Timescale.DAYS -> buildForDays(records, aggregationMode)
                        Timescale.WEEKS -> buildForWeeks(records, aggregationMode)
                        Timescale.MONTHS -> buildForMonths(records, aggregationMode)
                    }
                    _viewState.value = _viewState.value.copy(charts = charts)
                }
            }
        }
    }

    private fun buildForDays(
        records: List<Record>,
        aggregationMode: DailyAggregationMode,
    ): List<TrendsChartUiModel> {
        val grouped = records.groupBy { it.timestamp.toLocalDate() }

        val days: List<LocalDate> = generateSequence(
            from = grouped.keys.minOrNull(),
            to = grouped.keys.maxOrNull(),
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

        return buildCharts(
            periods = days,
            recordsByPeriod = grouped,
            daysInPeriodProvider = { day: LocalDate ->
                daysInPeriodForMode(
                    period = day,
                    recordsByPeriod = grouped,
                    calendarDays = listOf(day),
                    aggregationMode = aggregationMode,
                )
            },
            labelProvider = ::dayLabel,
            isCurrentPeriod = { false },
        )
    }

    private fun buildForWeeks(
        records: List<Record>,
        aggregationMode: DailyAggregationMode,
    ): List<TrendsChartUiModel> {
        val weekFields = WeekFields.of(Locale.getDefault())

        val grouped = records.groupBy { record ->
            record.timestamp.toLocalDate().with(weekFields.dayOfWeek(), 1)
        }

        val weeks: List<LocalDate> = generateSequence(
            from = grouped.keys.minOrNull(),
            to = grouped.keys.maxOrNull(),
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

        return buildCharts(
            periods = weeks,
            recordsByPeriod = grouped,
            daysInPeriodProvider = { weekStart: LocalDate ->
                val calendarDays = (0L..6L).map { weekStart.plusDays(it) }

                daysInPeriodForMode(
                    period = weekStart,
                    recordsByPeriod = grouped,
                    calendarDays = calendarDays,
                    aggregationMode = aggregationMode,
                )
            },
            elapsedDaysInCurrentPeriodProvider = { weekStart: LocalDate ->
                if (today in weekStart..weekStart.plusDays(6)) {
                    val elapsedCalendarDays: List<LocalDate> =
                        generateSequence(weekStart) { it.plusDays(1) }
                            .takeWhile { it <= today }
                            .toList()

                    daysInPeriodForMode(
                        period = weekStart,
                        recordsByPeriod = grouped,
                        calendarDays = elapsedCalendarDays,
                        aggregationMode = aggregationMode,
                    )
                } else {
                    emptyList()
                }
            },
            labelProvider = ::weekLabel,
            isCurrentPeriod = { start ->
                today in start..start.plusDays(6)
            },
        )
    }

    private fun buildForMonths(
        records: List<Record>,
        aggregationMode: DailyAggregationMode,
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

        return buildCharts(
            periods = months,
            recordsByPeriod = recordsByPeriod,
            daysInPeriodProvider = { ym: YearMonth ->
                val calendarDays =
                    (1..ym.lengthOfMonth()).map { day -> ym.atDay(day) }

                daysInPeriodForMode(
                    period = ym,
                    recordsByPeriod = recordsByPeriod,
                    calendarDays = calendarDays,
                    aggregationMode = aggregationMode,
                )
            },
            elapsedDaysInCurrentPeriodProvider = { ym: YearMonth ->
                if (ym == thisMonth) {
                    val elapsedCalendarDays: List<LocalDate> =
                        (1..todayDate.dayOfMonth)
                            .map { day -> ym.atDay(day) }

                    daysInPeriodForMode(
                        period = ym,
                        recordsByPeriod = recordsByPeriod,
                        calendarDays = elapsedCalendarDays,
                        aggregationMode = aggregationMode,
                    )
                } else {
                    emptyList()
                }
            },
            labelProvider = ::monthLabel,
            isCurrentPeriod = { ym ->
                ym == thisMonth
            },
        )
    }

    private fun <K : Comparable<K>> buildCharts(
        periods: List<K>,
        recordsByPeriod: Map<K, List<Record>>,
        daysInPeriodProvider: (K) -> List<LocalDate>,
        elapsedDaysInCurrentPeriodProvider: ((K) -> List<LocalDate>)? = null,
        labelProvider: (K) -> String,
        isCurrentPeriod: (K) -> Boolean,
    ): List<TrendsChartUiModel> {

        fun agg(valueProvider: (Record) -> Float?) =
            computePeriodAverages(
                periods = periods,
                recordsByPeriod = recordsByPeriod,
                daysInPeriodProvider = daysInPeriodProvider,
                elapsedDaysInCurrentPeriodProvider = elapsedDaysInCurrentPeriodProvider,
                valueProvider = valueProvider,
                labelProvider = labelProvider,
                isCurrentPeriod = isCurrentPeriod,
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
                Triple(" └>of which sugar (g)", Color(0xFFFFB74D), agg { it.template.nutrientBreakdown.ofWhichSugar }),
                Triple("     └>of which added sugar (g)", Color(0xFFFF802C), agg { it.template.nutrientBreakdown.ofWhichAddedSugar })
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

    private fun <K> daysInPeriodForMode(
        period: K,
        recordsByPeriod: Map<K, List<Record>>,
        calendarDays: List<LocalDate>,
        aggregationMode: DailyAggregationMode,
    ): List<LocalDate> {
        val recordsForPeriod = recordsByPeriod[period].orEmpty()

        return when (aggregationMode) {
            DailyAggregationMode.CALENDAR_DAYS ->
                calendarDays

            DailyAggregationMode.LOGGED_DAYS ->
                recordsForPeriod
                    .map { it.timestamp.toLocalDate() }
                    .distinct()

            DailyAggregationMode.QUALIFIED_DAYS -> {
                val calorieTotals = calorieTotalsByDay(recordsForPeriod)

                calorieTotals
                    .filterValues { it >= appPrefs.qualifiedAggregationThreshold }   // ← hardcoded threshold
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
     * - Average is computed over days returned by [daysInPeriodProvider]
     *   (days with no records contribute 0).
     *
     * Current (in-progress) period:
     * - Value is computed as a *projection* by averaging over elapsed days only
     *   (returned by [elapsedDaysInCurrentPeriodProvider]).
     * - This prevents the current bucket being artificially deflated by future days being treated as 0.
     *
     * @param K the type of period key (e.g. [YearMonth], [LocalDate], or an iso-week pair).
     * @param periods ordered list of period keys defining the x-axis of the chart.
     * @param recordsByPeriod records pre-grouped by the same period keys.
     * @param daysInPeriodProvider days that contribute to completed-period averages (denominator).
     * @param elapsedDaysInCurrentPeriodProvider days elapsed so far for current period (projection denominator).
     * @param minElapsedDaysForPrediction minimum elapsed days required to show a projected value.
     * @param valueProvider extracts the nutrient value from a [Record], or `null` if the record has no value.
     * @param labelProvider produces the human-readable x-axis label for a period.
     * @param isCurrentPeriod returns `true` if the given period represents the current (incomplete) time period.
     * @return a pair of (completed data points, current-period data point or `null`).
     */
    private fun <K> computePeriodAverages(
        periods: List<K>,
        recordsByPeriod: Map<K, List<Record>>,
        daysInPeriodProvider: (K) -> List<LocalDate>,
        elapsedDaysInCurrentPeriodProvider: ((K) -> List<LocalDate>)?,
        minElapsedDaysForPrediction: Int = 2,
        valueProvider: (Record) -> Float?,
        labelProvider: (K) -> String,
        isCurrentPeriod: (K) -> Boolean,
    ): Pair<List<ChartDataPoint>, ChartDataPoint?> {
        val points: List<Pair<K, ChartDataPoint>> = periods.mapIndexed { index, key ->
            val dailyTotals: Map<LocalDate, Float> =
                recordsByPeriod[key]
                    ?.mapNotNull { r ->
                        valueProvider(r)?.let { value ->
                            r.timestamp.toLocalDate() to value
                        }
                    }
                    ?.groupBy({ it.first }, { it.second })
                    ?.mapValues { (_, values) -> values.sum() }
                    ?: emptyMap()

            val contributingDays: List<LocalDate> =
                if (isCurrentPeriod(key)) {
                    elapsedDaysInCurrentPeriodProvider!!.invoke(key)
                } else {
                    daysInPeriodProvider(key)
                }

            val contributingValues: List<Float> =
                contributingDays.map { day -> dailyTotals[day] ?: 0f }

            val avg: Double? =
                if (isCurrentPeriod(key)) {
                    if (contributingValues.size >= minElapsedDaysForPrediction) {
                        contributingValues.average()
                    } else {
                        null
                    }
                } else {
                    contributingValues
                        .takeIf { it.isNotEmpty() }
                        ?.average()
                }

            key to ChartDataPoint(
                index = index,
                label = labelProvider(key),
                value = avg
            )
        }

        val last = points.lastOrNull() ?: return emptyList<ChartDataPoint>() to null

        return if (isCurrentPeriod(last.first)) {
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
}
