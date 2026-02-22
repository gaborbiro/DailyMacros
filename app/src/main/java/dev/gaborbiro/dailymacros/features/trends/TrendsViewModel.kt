package dev.gaborbiro.dailymacros.features.trends

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.trends.model.DailyAggregationMode
import dev.gaborbiro.dailymacros.features.trends.model.TrendsChartUiModel
import dev.gaborbiro.dailymacros.features.trends.model.ChartDataPoint
import dev.gaborbiro.dailymacros.features.trends.model.ChartDataset
import dev.gaborbiro.dailymacros.features.trends.model.TimeScale
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
        recordsJob = observeRecords(TimeScale.DAYS)
    }

    fun onTimeScaleSelected(timeScale: TimeScale) {
        recordsJob.cancel()
        recordsJob = observeRecords(timeScale)
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

    fun onAggregationModeChanged(dailyAggregationMode: DailyAggregationMode, timeScale: TimeScale) {
        appPrefs.aggregationMode = mapper.map(dailyAggregationMode)
        recordsJob.cancel()
        recordsJob = observeRecords(timeScale)
    }

    fun onAggregationThresholdChanged(threshold: Long, timeScale: TimeScale) {
        appPrefs.qualifiedAggregationThreshold = threshold
        recordsJob.cancel()
        recordsJob = observeRecords(timeScale)
    }

    private fun observeRecords(timeScale: TimeScale): Job {
        // Re-launch collection each time scale changes
        return viewModelScope.launch {
            // Use the same flow as overview (no search term = all records)
            recordsRepository.getFlowBySearchTerm(null).collect { records ->
                if (records.isEmpty()) {
                    _viewState.value = _viewState.value.copy(charts = emptyList())
                } else {
                    val aggregationMode = mapper.map(appPrefs.aggregationMode)
                    val charts = when (timeScale) {
                        TimeScale.DAYS -> buildForDays(records, aggregationMode)
                        TimeScale.WEEKS -> buildForWeeks(records, aggregationMode)
                        TimeScale.MONTHS -> buildForMonths(records, aggregationMode)
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

        val today = LocalDate.now()

        return buildCharts(
            buckets = days,
            grouped = grouped,
            daysInBucket = { day: LocalDate ->
                daysInKeyForMode(
                    key = day,
                    grouped = grouped,
                    calendarDays = listOf(day),
                    aggregationMode = aggregationMode,
                )
            },
            labelFor = ::dayLabel,
            isOngoing = { it == today },
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
            buckets = weeks,
            grouped = grouped,
            daysInBucket = { weekStart: LocalDate ->
                val calendarDays = (0L..6L).map { weekStart.plusDays(it) }

                daysInKeyForMode(
                    key = weekStart,
                    grouped = grouped,
                    calendarDays = calendarDays,
                    aggregationMode = aggregationMode,
                )
            },
            labelFor = ::weekLabel,
            isOngoing = { start ->
                today in start..start.plusDays(6)
            },
        )
    }

    private fun buildForMonths(
        records: List<Record>,
        aggregationMode: DailyAggregationMode,
    ): List<TrendsChartUiModel> {
        val grouped = records.groupBy { YearMonth.from(it.timestamp.toLocalDate()) }

        val months: List<YearMonth> = generateSequence(
            from = grouped.keys.minOrNull(),
            to = grouped.keys.maxOrNull(),
            step = { it.plusMonths(1) }
        )

        fun monthLabel(ym: YearMonth): String =
            ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

        val thisMonth = YearMonth.now()

        return buildCharts(
            buckets = months,
            grouped = grouped,
            daysInBucket = { ym: YearMonth ->
                val calendarDays =
                    (1..ym.lengthOfMonth()).map { day -> ym.atDay(day) }

                daysInKeyForMode(
                    key = ym,
                    grouped = grouped,
                    calendarDays = calendarDays,
                    aggregationMode = aggregationMode,
                )
            },
            labelFor = ::monthLabel,
            isOngoing = { ym ->
                ym == thisMonth
            },
        )
    }

    private fun <K : Comparable<K>> buildCharts(
        buckets: List<K>,
        grouped: Map<K, List<Record>>,
        daysInBucket: (K) -> List<LocalDate>,
        labelFor: (K) -> String,
        isOngoing: (K) -> Boolean,
    ): List<TrendsChartUiModel> {

        fun agg(selector: (Record) -> Float?) =
            aggregateSeries(
                keys = buckets,
                grouped = grouped,
                daysInKey = daysInBucket,
                selector = selector,
                makeLabel = labelFor,
                isOngoing = isOngoing,
            )

        return datasetsOf(
            listOf(
                Triple("Calories (kcal)", Color(0xFF8AB4F8), agg { it.template.nutrientsBreakdown?.calories?.toFloat() })
            ),
            listOf(
                Triple("Protein (g)", Color(0xFF81C995), agg { it.template.nutrientsBreakdown?.protein })
            ),
            listOf(
                Triple("Carbs (g)", Color(0xFFFFC278), agg { it.template.nutrientsBreakdown?.carbs }),
                Triple(" └>of which sugar (g)", Color(0xFFFFB74D), agg { it.template.nutrientsBreakdown?.ofWhichSugar }),
                Triple("     └>of which added sugar (g)", Color(0xFFFF802C), agg { it.template.nutrientsBreakdown?.ofWhichAddedSugar })
            ),
            listOf(
                Triple("Fat (g)", Color(0xFFFFA6A6), agg { it.template.nutrientsBreakdown?.fat }),
                Triple(" └>of which saturated fat (g)", Color(0xFFE57373), agg { it.template.nutrientsBreakdown?.ofWhichSaturated })
            ),
            listOf(
                Triple("Salt (g)", Color(0xFFB39DDB), agg { it.template.nutrientsBreakdown?.salt })
            ),
            listOf(
                Triple("Fibre (g)", Color(0xFF4DB6AC), agg { it.template.nutrientsBreakdown?.fibre })
            ),
        )
    }

    private fun <K> daysInKeyForMode(
        key: K,
        grouped: Map<K, List<Record>>,
        calendarDays: List<LocalDate>,
        aggregationMode: DailyAggregationMode,
    ): List<LocalDate> {
        val recordsForKey = grouped[key].orEmpty()

        return when (aggregationMode) {
            DailyAggregationMode.CALENDAR_DAYS ->
                calendarDays

            DailyAggregationMode.LOGGED_DAYS ->
                recordsForKey
                    .map { it.timestamp.toLocalDate() }
                    .distinct()

            DailyAggregationMode.QUALIFIED_DAYS -> {
                val calorieTotals = calorieTotalsByDay(recordsForKey)

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
                record.template.nutrientsBreakdown?.calories?.toFloat()?.let {
                    record.timestamp.toLocalDate() to it
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }


    private fun <K> aggregateSeries(
        keys: List<K>,
        grouped: Map<K, List<Record>>,
        daysInKey: (K) -> List<LocalDate>,
        selector: (Record) -> Float?,
        makeLabel: (K) -> String,
        isOngoing: (K) -> Boolean,
    ): Pair<List<ChartDataPoint>, ChartDataPoint?> {
        val points = keys.mapIndexed { index, key ->
            val dailyTotals: Map<LocalDate, Float> =
                grouped[key]
                    ?.mapNotNull { r ->
                        selector(r)?.let { value ->
                            r.timestamp.toLocalDate() to value
                        }
                    }
                    ?.groupBy({ it.first }, { it.second })
                    ?.mapValues { (_, values) -> values.sum() }
                    ?: emptyMap()

            val contributingValues =
                daysInKey(key).map { day -> dailyTotals[day] ?: 0f }

            val avg = contributingValues
                .takeIf { it.isNotEmpty() }
                ?.average()

            key to ChartDataPoint(
                index = index,
                label = makeLabel(key),
                value = avg
            )
        }

        val last = points.lastOrNull() ?: return emptyList<ChartDataPoint>() to null

        return if (isOngoing(last.first)) {
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
                    val (set: List<ChartDataPoint>, last) = points
                    ChartDataset(
                        name = name,
                        color = color,
                        set = set,
                        now = last
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
