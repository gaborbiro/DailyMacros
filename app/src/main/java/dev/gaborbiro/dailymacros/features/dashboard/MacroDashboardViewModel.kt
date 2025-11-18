package dev.gaborbiro.dailymacros.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.dashboard.views.MacroDataPoint
import dev.gaborbiro.dailymacros.features.dashboard.views.MacroDataset
import dev.gaborbiro.dailymacros.features.dashboard.views.TimeScale
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

internal data class MacroDashboardViewState(
    val scale: TimeScale = TimeScale.DAYS,
    val datasets: List<MacroDataset> = emptyList(),
)

internal class MacroDashboardViewModel(
    private val recordsRepository: RecordsRepository,
) : ViewModel() {

    private val _viewState: MutableStateFlow<MacroDashboardViewState> =
        MutableStateFlow(MacroDashboardViewState())
    val viewState: StateFlow<MacroDashboardViewState> = _viewState.asStateFlow()

    init {
        // start with daily scale
        observeRecords()
    }

    fun onScaleSelected(scale: TimeScale) {
        if (scale == _viewState.value.scale) return
        _viewState.value = _viewState.value.copy(scale = scale)
        observeRecords()
    }

    private fun observeRecords() {
        // Re-launch collection each time scale changes
        viewModelScope.launch {
            // Use the same flow as overview (no search term = all records)
            recordsRepository.getFlowBySearchTerm(null).collect { records ->
                val datasets = buildDatasets(records, _viewState.value.scale)
                _viewState.value = _viewState.value.copy(datasets = datasets)
            }
        }
    }

    private fun buildDatasets(records: List<Record>, scale: TimeScale): List<MacroDataset> {
        if (records.isEmpty()) return emptyList()

        val weekFields = WeekFields.of(Locale.getDefault())

        // Aggregate data and generate nicely formatted labels per scale
        fun buildForDays(): List<MacroDataset> {
            val grouped: Map<LocalDate, List<Record>> = records.groupBy { it.timestamp.toLocalDate() }
            val days = grouped.keys.sorted()

            fun dayLabel(date: LocalDate): String {
                return if (date.dayOfMonth == 1) {
                    "${date.dayOfMonth}/${date.monthValue}"
                } else {
                    date.dayOfMonth.toString()
                }
            }

            fun sumFor(selector: (Record) -> Float?): List<MacroDataPoint> {
                return days.map { date ->
                    val recordsForDay = grouped[date].orEmpty()
                    val sum = recordsForDay.mapNotNull(selector).sum()
                    MacroDataPoint(label = dayLabel(date), value = sum)
                }
            }

            val calories = sumFor { it.template.macros?.calories?.toFloat() }
            val protein = sumFor { it.template.macros?.protein }
            val carbs = sumFor { it.template.macros?.carbs }
            val fat = sumFor { it.template.macros?.fat }

            return listOf(
                MacroDataset("Calories (kcal)", androidx.compose.ui.graphics.Color(0xFF8AB4F8), calories),
                MacroDataset("Protein (g)", androidx.compose.ui.graphics.Color(0xFF81C995), protein),
                MacroDataset("Carbs (g)", androidx.compose.ui.graphics.Color(0xFFFFC278), carbs),
                MacroDataset("Fat (g)", androidx.compose.ui.graphics.Color(0xFFFFA6A6), fat),
            )
        }

        fun buildForWeeks(): List<MacroDataset> {
            // Use the first day of the week (typically Monday) as key
            val grouped: Map<LocalDate, List<Record>> = records.groupBy { record ->
                val date = record.timestamp.toLocalDate()
                // Day-of-week 1 in WeekFields is the first day (e.g., Monday for ISO)
                date.with(weekFields.dayOfWeek(), 1)
            }
            val weekStarts = grouped.keys.sorted()

            fun weekLabel(weekStart: LocalDate): String {
                val weekEnd = weekStart.plusDays(6)
                val startDay = weekStart.dayOfMonth
                val endDay = weekEnd.dayOfMonth
                return if (weekStart.month == weekEnd.month) {
                    // Same month: 20-26
                    "$startDay-$endDay"
                } else {
                    // Cross-month: 27-2 Oct
                    val monthAbbrev = weekEnd.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    "$startDay-$endDay $monthAbbrev"
                }
            }

            fun sumFor(selector: (Record) -> Float?): List<MacroDataPoint> {
                return weekStarts.map { weekStart ->
                    val recordsForWeek = grouped[weekStart].orEmpty()
                    val sum = recordsForWeek.mapNotNull(selector).sum()
                    MacroDataPoint(label = weekLabel(weekStart), value = sum)
                }
            }

            val calories = sumFor { it.template.macros?.calories?.toFloat() }
            val protein = sumFor { it.template.macros?.protein }
            val carbs = sumFor { it.template.macros?.carbs }
            val fat = sumFor { it.template.macros?.fat }

            return listOf(
                MacroDataset("Calories (kcal)", androidx.compose.ui.graphics.Color(0xFF8AB4F8), calories),
                MacroDataset("Protein (g)", androidx.compose.ui.graphics.Color(0xFF81C995), protein),
                MacroDataset("Carbs (g)", androidx.compose.ui.graphics.Color(0xFFFFC278), carbs),
                MacroDataset("Fat (g)", androidx.compose.ui.graphics.Color(0xFFFFA6A6), fat),
            )
        }

        fun buildForMonths(): List<MacroDataset> {
            val grouped = records.groupBy { record ->
                YearMonth.from(record.timestamp.toLocalDate())
            }
            val months = grouped.keys.sorted()

            fun monthLabel(month: YearMonth): String {
                // e.g. "Jan", "Feb" with first letter capitalised per locale
                return month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }

            fun sumFor(selector: (Record) -> Float?): List<MacroDataPoint> {
                return months.map { ym ->
                    val recordsForMonth = grouped[ym].orEmpty()
                    val sum = recordsForMonth.mapNotNull(selector).sum()
                    MacroDataPoint(label = monthLabel(ym), value = sum)
                }
            }

            val calories = sumFor { it.template.macros?.calories?.toFloat() }
            val protein = sumFor { it.template.macros?.protein }
            val carbs = sumFor { it.template.macros?.carbs }
            val fat = sumFor { it.template.macros?.fat }

            return listOf(
                MacroDataset("Calories (kcal)", androidx.compose.ui.graphics.Color(0xFF8AB4F8), calories),
                MacroDataset("Protein (g)", androidx.compose.ui.graphics.Color(0xFF81C995), protein),
                MacroDataset("Carbs (g)", androidx.compose.ui.graphics.Color(0xFFFFC278), carbs),
                MacroDataset("Fat (g)", androidx.compose.ui.graphics.Color(0xFFFFA6A6), fat),
            )
        }

        return when (scale) {
            TimeScale.DAYS -> buildForDays()
            TimeScale.WEEKS -> buildForWeeks()
            TimeScale.MONTHS -> buildForMonths()
        }
    }
}
