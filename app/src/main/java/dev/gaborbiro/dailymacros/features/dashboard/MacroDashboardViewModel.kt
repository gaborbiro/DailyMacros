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
import java.time.ZonedDateTime
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

        // Group records according to scale
        val grouped: Map<String, List<Record>> = when (scale) {
            TimeScale.DAYS -> records.groupBy { record ->
                record.timestamp.toLocalDate().toString()
            }

            TimeScale.WEEKS -> records.groupBy { record ->
                val date = record.timestamp.toLocalDate()
                val week = date.get(weekFields.weekOfWeekBasedYear())
                "${date.year}-W$week"
            }

            TimeScale.MONTHS -> records.groupBy { record ->
                val date = record.timestamp.toLocalDate()
                "${date.year}-${"%02d".format(date.monthValue)}"
            }
        }

        val sortedKeys = grouped.keys.sorted()

        fun sumFor(selector: (Record) -> Float?): List<MacroDataPoint> {
            return sortedKeys.map { key ->
                val recordsForPeriod = grouped[key].orEmpty()
                val sum = recordsForPeriod.mapNotNull(selector).sum()
                MacroDataPoint(label = key, value = sum)
            }
        }

        // Aggregate from template macros (same approach used elsewhere in the app)
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
}
