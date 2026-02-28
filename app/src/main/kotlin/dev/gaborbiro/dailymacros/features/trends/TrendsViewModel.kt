package dev.gaborbiro.dailymacros.features.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.trends.model.DayQualifier
import dev.gaborbiro.dailymacros.features.trends.model.Timescale
import dev.gaborbiro.dailymacros.features.trends.model.TrendsSettingsUIModel
import dev.gaborbiro.dailymacros.features.trends.model.TrendsUiUpdates
import dev.gaborbiro.dailymacros.features.trends.model.TrendsUiState
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


internal class TrendsViewModel(
    private val recordsRepository: RecordsRepository,
    private val appPrefs: AppPrefs,
    private val mapper: TrendsUiMapper,
) : ViewModel() {

    private var recordsJob: Job

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<TrendsUiUpdates>()
    val uiUpdates: SharedFlow<TrendsUiUpdates> = _uiUpdates.asSharedFlow()

    init {
        recordsJob = observeRecords(Timescale.DAYS)
    }

    fun onTimescaleSelected(timescale: Timescale) {
        recordsJob.cancel()
        recordsJob = observeRecords(timescale)
    }

    fun onBackNavigate() {
        viewModelScope.launch {
            _uiUpdates.emit(TrendsUiUpdates.NavigateBack)
        }
    }

    fun onSettingsActionButtonClicked() {
        _uiState.update {
            it.copy(
                settings = TrendsSettingsUIModel.Show(
                    dayQualifier = mapper.map(appPrefs.dayQualificationMode),
                    qualifiedDaysThreshold = appPrefs.qualifyingCalorieThreshold,
                )
            )
        }
    }

    fun onSettingsCloseRequested() {
        _uiState.update {
            it.copy(settings = TrendsSettingsUIModel.Hidden)
        }
    }

    fun onAggregationModeChanged(dayQualifier: DayQualifier, timescale: Timescale) {
        appPrefs.dayQualificationMode = mapper.map(dayQualifier)
        recordsJob.cancel()
        recordsJob = observeRecords(timescale)
    }

    fun onAggregationThresholdChanged(threshold: Long, timescale: Timescale) {
        appPrefs.qualifyingCalorieThreshold = threshold
        recordsJob.cancel()
        recordsJob = observeRecords(timescale)
    }

    private fun observeRecords(timescale: Timescale): Job {
        // Re-launch collection each timescale changes
        return viewModelScope.launch {
            // Use the same flow as overview (no search term = all records)
            recordsRepository.observeRecords(null).collect { records ->
                if (records.isEmpty()) {
                    _uiState.update {
                        it.copy(charts = emptyList())
                    }
                } else {
                    val aggregationMode = mapper.map(appPrefs.dayQualificationMode)
                    val charts = when (timescale) {
                        Timescale.DAYS -> mapper.mapDaysCharts(records, aggregationMode)
                        Timescale.WEEKS -> mapper.mapWeeksCharts(records, aggregationMode)
                        Timescale.MONTHS -> mapper.mapMonthsCharts(records, aggregationMode)
                    }
                    _uiState.update {
                        it.copy(charts = charts)
                    }
                }
            }
        }
    }
}
