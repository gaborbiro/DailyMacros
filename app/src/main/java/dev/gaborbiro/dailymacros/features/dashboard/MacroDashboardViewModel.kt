package dev.gaborbiro.dailymacros.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.dashboard.model.MacroDashboardViewState
import dev.gaborbiro.dailymacros.features.dashboard.model.MacroDataPoint
import dev.gaborbiro.dailymacros.features.dashboard.model.MacroDataset
import dev.gaborbiro.dailymacros.features.dashboard.model.TimeScale
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

internal class MacroDashboardViewModel(
    private val recordsRepository: RecordsRepository,
) : ViewModel() {

    private val _viewState: MutableStateFlow<MacroDashboardViewState> =
        MutableStateFlow(MacroDashboardViewState())
    val viewState: StateFlow<MacroDashboardViewState> = _viewState.asStateFlow()

    init {
        observeRecords(TimeScale.DAYS)
    }

    fun onScaleSelected(scale: TimeScale) {
        if (scale == _viewState.value.scale) return
        observeRecords(scale)
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

    private fun buildDatasets(records: List<Record>, scale: TimeScale): List<MacroDataset> {
        if (records.isEmpty()) return emptyList()

        val weekFields = WeekFields.of(Locale.getDefault())

        // Aggregate data and generate nicely formatted labels per scale
        fun buildForDays(): List<MacroDataset> {
            val grouped: Map<LocalDate, List<Record>> = records.groupBy { it.timestamp.toLocalDate() }

            // Build a continuous range from earliest to latest day so missing days
            // appear on the X axis with zero values.
            val minDay = grouped.keys.minOrNull() ?: return emptyList()
            val maxDay = grouped.keys.maxOrNull() ?: return emptyList()
            val days = generateSequence(minDay) { prev ->
                val next = prev.plusDays(1)
                if (next <= maxDay) next else null
            }.toList()

            val weekFields = WeekFields.of(Locale.getDefault())
            val firstDayOfWeek = weekFields.firstDayOfWeek
            val lastDayOfWeek = firstDayOfWeek.minus(1)
            fun dayLabel(date: LocalDate): String {
                val base = if (date.dayOfMonth == 1) {
                    "${date.dayOfMonth}/${date.monthValue}"
                } else {
                    date.dayOfMonth.toString()
                }

                return when (date.dayOfWeek) {
                    firstDayOfWeek -> "•$base"   // week start (locale-aware)
                    lastDayOfWeek -> "$base•"   // week end   (locale-aware)
                    else -> base
                }
            }

            fun sumFor(selector: (Record) -> Float?): List<MacroDataPoint> {
                return days.map { date ->
                    val recordsForDay = grouped[date].orEmpty()
                    val values = recordsForDay.mapNotNull(selector)
                    val sum = values.takeIf { it.isNotEmpty() }?.sum()
                    MacroDataPoint(label = dayLabel(date), value = sum)
                }
            }

            val calories = sumFor { it.template.macros?.calories?.toFloat() }
            val protein = sumFor { it.template.macros?.protein }
            val carbs = sumFor { it.template.macros?.carbs }
            val sugar = sumFor { it.template.macros?.ofWhichSugar }
            val fat = sumFor { it.template.macros?.fat }
            val saturated = sumFor { it.template.macros?.ofWhichSaturated }
            val salt = sumFor { it.template.macros?.salt }
            val fibre = sumFor { it.template.macros?.fibre }

            return listOf(
                MacroDataset("Calories (kcal)", androidx.compose.ui.graphics.Color(0xFF8AB4F8), calories),
                MacroDataset("Protein (g)", androidx.compose.ui.graphics.Color(0xFF81C995), protein),
                MacroDataset("Carbs (g)", androidx.compose.ui.graphics.Color(0xFFFFC278), carbs),
                MacroDataset("    of which sugar (g)", androidx.compose.ui.graphics.Color(0xFFFFB74D), sugar),
                MacroDataset("Fat (g)", androidx.compose.ui.graphics.Color(0xFFFFA6A6), fat),
                MacroDataset("    of which saturated fat (g)", androidx.compose.ui.graphics.Color(0xFFE57373), saturated),
                MacroDataset("Salt (g)", androidx.compose.ui.graphics.Color(0xFFB39DDB), salt),
                MacroDataset("Fibre (g)", androidx.compose.ui.graphics.Color(0xFF4DB6AC), fibre),
            )
        }

        fun buildForWeeks(): List<MacroDataset> {
            // Use the first day of the week (typically Monday) as key
            val grouped: Map<LocalDate, List<Record>> = records.groupBy { record ->
                val date = record.timestamp.toLocalDate()
                // Day-of-week 1 in WeekFields is the first day (e.g., Monday for ISO)
                date.with(weekFields.dayOfWeek(), 1)
            }

            // Build a continuous sequence of week starts between min and max so
            // entirely missing weeks still appear on the X axis with zero values.
            val minWeekStart = grouped.keys.minOrNull() ?: return emptyList()
            val maxWeekStart = grouped.keys.maxOrNull() ?: return emptyList()
            val weekStarts = generateSequence(minWeekStart) { prev ->
                val next = prev.plusWeeks(1)
                if (next <= maxWeekStart) next else null
            }.toList()

            fun weekLabel(weekStart: LocalDate): String {
                val weekEnd = weekStart.plusDays(6)
                val startDay = weekStart.dayOfMonth
                val endDay = weekEnd.dayOfMonth

                // Does this week contain the first day of any month?
                val includesFirstOfMonth = (0L..6L).any { offset ->
                    val date = weekStart.plusDays(offset)
                    date.dayOfMonth == 1
                }

                return if (weekStart.month == weekEnd.month && !includesFirstOfMonth) {
                    // Same month and does NOT include a 1st-of-month day: 20-26
                    "$startDay-$endDay"
                } else {
                    // Cross-month OR includes 1st of a month: append end month, e.g. 1-7 Sep, 27-2 Oct
                    val monthAbbrev = weekEnd.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    "$startDay-$endDay $monthAbbrev"
                }
            }

            fun sumFor(selector: (Record) -> Float?): List<MacroDataPoint> {
                return weekStarts.map { weekStart ->
                    val recordsForWeek = grouped[weekStart].orEmpty()
                    val values = recordsForWeek.mapNotNull(selector)
                    val sum = values.takeIf { it.isNotEmpty() }?.sum()
                    MacroDataPoint(label = weekLabel(weekStart), value = sum)
                }
            }

            val calories = sumFor { it.template.macros?.calories?.toFloat() }
            val protein = sumFor { it.template.macros?.protein }
            val carbs = sumFor { it.template.macros?.carbs }
            val sugar = sumFor { it.template.macros?.ofWhichSugar }
            val fat = sumFor { it.template.macros?.fat }
            val saturated = sumFor { it.template.macros?.ofWhichSaturated }
            val salt = sumFor { it.template.macros?.salt }
            val fibre = sumFor { it.template.macros?.fibre }

            return listOf(
                MacroDataset("Calories (kcal)", androidx.compose.ui.graphics.Color(0xFF8AB4F8), calories),
                MacroDataset("Protein (g)", androidx.compose.ui.graphics.Color(0xFF81C995), protein),
                MacroDataset("Carbs (g)", androidx.compose.ui.graphics.Color(0xFFFFC278), carbs),
                MacroDataset("    of which sugar (g)", androidx.compose.ui.graphics.Color(0xFFFFB74D), sugar),
                MacroDataset("Fat (g)", androidx.compose.ui.graphics.Color(0xFFFFA6A6), fat),
                MacroDataset("    of which saturated fat (g)", androidx.compose.ui.graphics.Color(0xFFE57373), saturated),
                MacroDataset("Salt (g)", androidx.compose.ui.graphics.Color(0xFFB39DDB), salt),
                MacroDataset("Fibre (g)", androidx.compose.ui.graphics.Color(0xFF4DB6AC), fibre),
            )
        }

        fun buildForMonths(): List<MacroDataset> {
            val grouped = records.groupBy { record ->
                YearMonth.from(record.timestamp.toLocalDate())
            }

            // Build a continuous sequence of YearMonth between min and max so
            // entirely missing months still appear on the X axis with zero values.
            val minMonth = grouped.keys.minOrNull() ?: return emptyList()
            val maxMonth = grouped.keys.maxOrNull() ?: return emptyList()
            val months = generateSequence(minMonth) { prev ->
                val next = prev.plusMonths(1)
                if (next <= maxMonth) next else null
            }.toList()

            fun monthLabel(month: YearMonth): String {
                // e.g. "Jan", "Feb" with first letter capitalised per locale
                return month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }

            fun sumFor(selector: (Record) -> Float?): List<MacroDataPoint> {
                return months.map { ym ->
                    val recordsForMonth = grouped[ym].orEmpty()
                    val values = recordsForMonth.mapNotNull(selector)
                    val sum = values.takeIf { it.isNotEmpty() }?.sum()
                    MacroDataPoint(label = monthLabel(ym), value = sum)
                }
            }

            val calories = sumFor { it.template.macros?.calories?.toFloat() }
            val protein = sumFor { it.template.macros?.protein }
            val carbs = sumFor { it.template.macros?.carbs }
            val sugar = sumFor { it.template.macros?.ofWhichSugar }
            val fat = sumFor { it.template.macros?.fat }
            val saturated = sumFor { it.template.macros?.ofWhichSaturated }
            val salt = sumFor { it.template.macros?.salt }
            val fibre = sumFor { it.template.macros?.fibre }

            return listOf(
                MacroDataset("Calories (kcal)", androidx.compose.ui.graphics.Color(0xFF8AB4F8), calories),
                MacroDataset("Protein (g)", androidx.compose.ui.graphics.Color(0xFF81C995), protein),
                MacroDataset("Carbs (g)", androidx.compose.ui.graphics.Color(0xFFFFC278), carbs),
                MacroDataset("    of which sugar (g)", androidx.compose.ui.graphics.Color(0xFFFFB74D), sugar),
                MacroDataset("Fat (g)", androidx.compose.ui.graphics.Color(0xFFFFA6A6), fat),
                MacroDataset("    of which saturated fat (g)", androidx.compose.ui.graphics.Color(0xFFE57373), saturated),
                MacroDataset("Salt (g)", androidx.compose.ui.graphics.Color(0xFFB39DDB), salt),
                MacroDataset("Fibre (g)", androidx.compose.ui.graphics.Color(0xFF4DB6AC), fibre),
            )
        }

        return when (scale) {
            TimeScale.DAYS -> buildForDays()
            TimeScale.WEEKS -> buildForWeeks()
            TimeScale.MONTHS -> buildForMonths()
        }
    }
}
