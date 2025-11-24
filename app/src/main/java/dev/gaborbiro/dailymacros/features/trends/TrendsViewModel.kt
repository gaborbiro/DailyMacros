package dev.gaborbiro.dailymacros.features.trends

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.trends.model.MacroChartDataPoint
import dev.gaborbiro.dailymacros.features.trends.model.MacroChartDataset
import dev.gaborbiro.dailymacros.features.trends.model.TimeScale
import dev.gaborbiro.dailymacros.features.trends.model.TrendsViewState
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
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
) : ViewModel() {

    private val _viewState: MutableStateFlow<TrendsViewState> =
        MutableStateFlow(TrendsViewState())
    val viewState: StateFlow<TrendsViewState> = _viewState.asStateFlow()

    init {
        observeRecords(TimeScale.DAYS)
    }

    fun onScaleSelected(scale: TimeScale) {
        if (scale == _viewState.value.scale) return
        observeRecords(scale)
    }

    fun onBackNavigate() {
        navigator.navigateBack()
    }

    private fun observeRecords(scale: TimeScale) {
        // Re-launch collection each time scale changes
        viewModelScope.launch {
            // Use the same flow as overview (no search term = all records)
            recordsRepository.getFlowBySearchTerm(null).collect { records ->
                val datasets = buildDatasets(records, scale)
                _viewState.value = _viewState.value.copy(datasets = datasets, scale = scale)
            }
        }
    }

    private fun buildDatasets(records: List<Record>, scale: TimeScale): List<MacroChartDataset> {
        if (records.isEmpty()) return emptyList()

        val weekFields = WeekFields.of(Locale.getDefault())

        fun buildForDays(): List<MacroChartDataset> {
            val grouped = records.groupBy { it.timestamp.toLocalDate() }

            val days = continuousSequence(
                min = grouped.keys.minOrNull(),
                max = grouped.keys.maxOrNull(),
                advance = { it.plusDays(1) }
            )

            val weekFields = WeekFields.of(Locale.getDefault())
            val first = weekFields.firstDayOfWeek
            val last  = first.minus(1)

            fun dayLabel(date: LocalDate): String {
                val base = if (date.dayOfMonth == 1)
                    "${date.dayOfMonth}/${date.monthValue}"
                else
                    "${date.dayOfMonth}"

                return when (date.dayOfWeek) {
                    first -> "•$base"
                    last  -> "$base•"
                    else  -> base
                }
            }

            val today = LocalDate.now()

            return buildMacroDatasets(
                keys = days,
                grouped = grouped,
                labelFor = ::dayLabel,
                isOngoing = { it == today }
            )
        }

        fun buildForWeeks(): List<MacroChartDataset> {
            val grouped = records.groupBy { r ->
                r.timestamp.toLocalDate().with(weekFields.dayOfWeek(), 1)
            }

            val weeks = continuousSequence(
                min = grouped.keys.minOrNull(),
                max = grouped.keys.maxOrNull(),
                advance = { it.plusWeeks(1) }
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

            return buildMacroDatasets(
                keys = weeks,
                grouped = grouped,
                labelFor = ::weekLabel,
                isOngoing = { start ->
                    today in start..start.plusDays(6)
                }
            )
        }

        fun buildForMonths(): List<MacroChartDataset> {
            val grouped = records.groupBy { YearMonth.from(it.timestamp.toLocalDate()) }

            val months = continuousSequence(
                min = grouped.keys.minOrNull(),
                max = grouped.keys.maxOrNull(),
                advance = { it.plusMonths(1) }
            )

            fun monthLabel(ym: YearMonth): String =
                ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

            val thisMonth = YearMonth.now()

            return buildMacroDatasets(
                keys = months,
                grouped = grouped,
                labelFor = ::monthLabel,
                isOngoing = { ym ->
                    ym == thisMonth
                }
            )
        }

        return when (scale) {
            TimeScale.DAYS -> buildForDays()
            TimeScale.WEEKS -> buildForWeeks()
            TimeScale.MONTHS -> buildForMonths()
        }
    }

    private fun <K : Comparable<K>> buildMacroDatasets(
        keys: List<K>,
        grouped: Map<K, List<Record>>,
        labelFor: (K) -> String,
        isOngoing: (K) -> Boolean
    ): List<MacroChartDataset> {

        fun agg(selector: (Record) -> Float?) =
            aggregateSeries(
                keys = keys,
                grouped = grouped,
                selector = selector,
                makeLabel = labelFor,
                isOngoing = isOngoing
            )

        return datasetsOf(
            Triple("Calories (kcal)",                Color(0xFF8AB4F8), agg { it.template.macros?.calories?.toFloat() }),
            Triple("Protein (g)",                    Color(0xFF81C995), agg { it.template.macros?.protein }),
            Triple("Carbs (g)",                      Color(0xFFFFC278), agg { it.template.macros?.carbs }),
            Triple("    of which sugar (g)",         Color(0xFFFFB74D), agg { it.template.macros?.ofWhichSugar }),
            Triple("Fat (g)",                        Color(0xFFFFA6A6), agg { it.template.macros?.fat }),
            Triple("    of which saturated fat (g)", Color(0xFFE57373), agg { it.template.macros?.ofWhichSaturated }),
            Triple("Salt (g)",                       Color(0xFFB39DDB), agg { it.template.macros?.salt }),
            Triple("Fibre (g)",                      Color(0xFF4DB6AC), agg { it.template.macros?.fibre }),
        )
    }


    private fun <T : Comparable<T>> continuousSequence(
        min: T?,
        max: T?,
        advance: (T) -> T,
    ): List<T> {
        if (min == null || max == null) return emptyList()

        return generateSequence(min) { prev ->
            val next = advance(prev)
            if (next <= max) next else null
        }.toList()
    }

    private fun <K> aggregateSeries(
        keys: List<K>,
        grouped: Map<K, List<Record>>,
        selector: (Record) -> Float?,
        makeLabel: (K) -> String,
        isOngoing: (K) -> Boolean,
    ): Pair<List<MacroChartDataPoint>, MacroChartDataPoint?> {
        val points = keys.mapIndexed { index, key ->
            val sum = grouped[key]
                ?.mapNotNull(selector)
                ?.takeIf { it.isNotEmpty() }
                ?.sum()

            key to MacroChartDataPoint(index, makeLabel(key), sum)
        }

        val last = points.lastOrNull() ?: return emptyList<MacroChartDataPoint>() to null

        return if (isOngoing(last.first)) {
            points.dropLast(1).map { it.second } to last.second
        } else {
            points.map { it.second } to null
        }
    }

    private fun datasetsOf(
        vararg tuples: Triple<String, Color, Pair<List<MacroChartDataPoint>, MacroChartDataPoint?>>
    ): List<MacroChartDataset> =
        tuples.map { (name, color, points) ->
            val (set, last) = points
            MacroChartDataset(
                name = name,
                color = color,
                set = set,
                now = last
            )
        }
}
