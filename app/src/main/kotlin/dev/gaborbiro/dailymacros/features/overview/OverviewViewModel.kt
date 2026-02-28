package dev.gaborbiro.dailymacros.features.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.App
import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.common.RepeatRecordUseCase
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelBase
import dev.gaborbiro.dailymacros.features.common.workers.GetMacrosWorker
import dev.gaborbiro.dailymacros.features.overview.model.OverviewUiEvents
import dev.gaborbiro.dailymacros.features.overview.model.OverviewUiState
import dev.gaborbiro.dailymacros.features.widget.DiaryWidgetScreen
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import dev.gaborbiro.dailymacros.util.combine
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

internal class OverviewViewModel(
    private val recordsRepository: RecordsRepository,
    private val repeatRecordUseCase: RepeatRecordUseCase,
    private val settingsRepository: SettingsRepository,
    private val uiMapper: OverviewUiMapper,
    private val appPrefs: AppPrefs,
) : ViewModel() {

    private val _viewState: MutableStateFlow<OverviewUiState> =
        MutableStateFlow(OverviewUiState())
    val viewState: StateFlow<OverviewUiState> = _viewState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<OverviewUiEvents>()
    val uiEvents: SharedFlow<OverviewUiEvents> = _uiEvents.asSharedFlow()

    private companion object {
        val PAGE_SIZE = 14.days
    }

    private var sinceEpochMillis: Long = System.currentTimeMillis() - PAGE_SIZE.inWholeMilliseconds
    private var currentSearch: String? = null
    private var collectionJob: Job? = null
    private var previousRecordCount: Int = -1

    fun onSearchTermChanged(search: String?) {
        currentSearch = search
        // Reset paging window when the search term changes
        sinceEpochMillis = System.currentTimeMillis() - PAGE_SIZE.inWholeMilliseconds
        previousRecordCount = -1
        _viewState.update { it.copy(hasMoreData = true) }
        resubscribe(search)
    }

    fun onLoadMore() {
        if (!_viewState.value.hasMoreData || _viewState.value.isLoadingMore) return
        _viewState.update { it.copy(isLoadingMore = true) }
        sinceEpochMillis -= PAGE_SIZE.inWholeMilliseconds
        resubscribe(currentSearch)
    }

    private fun resubscribe(search: String?) {
        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            val sinceMillis = if (search.isNullOrBlank()) sinceEpochMillis else 0L
            recordsRepository.observeRecords(search, sinceEpochMillis = sinceMillis)
                .combine(flowOf(settingsRepository.getTargets()))
                .map { (records: List<Record>, targets: Targets) ->
                    if (search.isNullOrBlank()) {
                        uiMapper.map(records, targets)
                    } else {
                        uiMapper.mapSearchResults(records)
                    }
                }
                .collect { records: List<ListUiModelBase> ->
                    // Determine if there is more data to load.
                    // For search results we load everything, so no more pages.
                    // For the main list, if expanding the window didn't bring new
                    // records compared to the previous emission, we've hit the end.
                    val hasMore = if (!search.isNullOrBlank()) {
                        false
                    } else if (previousRecordCount >= 0 && records.size <= previousRecordCount) {
                        false
                    } else {
                        true
                    }
                    previousRecordCount = records.size

                    _viewState.update {
                        if (records.isNotEmpty()) {
                            it.copy(
                                items = records,
                                isLoadingMore = false,
                                hasMoreData = hasMore,
                                showSettingsButton = search.isNullOrBlank(),
                            )
                        } else {
                            it.copy(
                                items = records,
                                isLoadingMore = false,
                                hasMoreData = hasMore,
                                showAddWidgetButton = search.isNullOrBlank(),
                                showSettingsButton = search.isNullOrBlank(),
                            )
                        }
                    }
                    if (records.size == 2 && appPrefs.showCoachMark) {
                        appPrefs.showCoachMark = false
                        delay(2.seconds)
                        _viewState.update {
                            it.copy(
                                showCoachMark = true,
                                showSettingsButton = search.isNullOrBlank(),
                            )
                        }
                    }
                }
        }
    }

    fun onCoachMarkDismissed() {
        _viewState.update {
            it.copy(
                showCoachMark = false
            )
        }
    }

    fun onRepeatMenuItemTapped(recordId: Long) {
        viewModelScope.launch {
            repeatRecordUseCase.execute(recordId = recordId)
        }
        DiaryWidgetScreen.reload()
    }

    fun onAnalyseMacrosMenuItemTapped(recordId: Long) {
        viewModelScope.launch {
            GetMacrosWorker.cancelWorkRequest(
                appContext = App.appContext,
                recordId = recordId,
            )
            GetMacrosWorker.setWorkRequest(
                appContext = App.appContext,
                recordId = recordId,
                force = true,
            )
        }
    }

    fun onDetailsMenuItemTapped(recordId: Long) {
        viewModelScope.launch {
            _uiEvents.emit(OverviewUiEvents.EditRecord(recordId))
        }
    }

    fun onAddToQuickPicksMenuItemTapped(recordId: Long) {
        viewModelScope.launch {
            val templateId = recordsRepository.get(recordId)?.template?.dbId ?: return@launch
            recordsRepository.addQuickPickOverride(templateId, Template.QuickPickOverride.INCLUDE)
            DiaryWidgetScreen.reload()
        }
    }

    fun onDeleteMenuItemTapped(recordId: Long) {
        viewModelScope.launch {
            val oldRecord = recordsRepository.deleteRecord(recordId = recordId)
            _viewState.update {
                it.copy(
                    showUndoDeleteSnackbar = true,
                    recordToUndelete = oldRecord,
                )
            }
            DiaryWidgetScreen.reload()
            GetMacrosWorker.cancelWorkRequest(
                appContext = App.appContext,
                recordId = recordId,
            )
        }
    }

    fun onRecordImageTapped(recordId: Long) {
        viewModelScope.launch {
            _uiEvents.emit(OverviewUiEvents.ViewImage(recordId))
        }
    }

    fun onRecordBodyTapped(recordId: Long) {
        viewModelScope.launch {
            _uiEvents.emit(OverviewUiEvents.EditRecord(recordId))
        }
    }

    fun onUndoDeleteTapped() {
        viewModelScope.launch {
            recordsRepository.updateRecord(_viewState.value.recordToUndelete!!)
        }
        _viewState.update {
            it.copy(
                recordToUndelete = null,
            )
        }
        DiaryWidgetScreen.reload()
    }

    fun onUndoDeleteDismissed() {
        deleteTemplate(_viewState.value.recordToUndelete!!.template.dbId)
        _viewState.update {
            it.copy(
                recordToUndelete = null,
            )
        }
    }

    fun onSettingsButtonTapped() {
        viewModelScope.launch {
            _uiEvents.emit(OverviewUiEvents.OpenSettingsScreen)
        }
        _viewState.update {
            it.copy(
                showCoachMark = false
            )
        }
    }

    fun onTrendsButtonTapped() {
        viewModelScope.launch {
            _uiEvents.emit(OverviewUiEvents.OpenTrendsScreen)
        }
    }

    private fun deleteTemplate(templateId: Long) {
        GlobalScope.launch {
            val (templateDeleted, imageDeleted) = recordsRepository.deleteTemplateIfUnused(
                templateId = templateId,
                imageToo = true,
            )
            Log.d(
                "Notes",
                "template deleted: $templateDeleted, image deleted: $imageDeleted"
            )
            DiaryWidgetScreen.reload()
        }
    }

    fun onUndoDeleteSnackbarShown() {
        _viewState.update {
            it.copy(
                showUndoDeleteSnackbar = false
            )
        }
    }

    fun finalizePendingUndos() {
        _viewState.value.recordToUndelete?.let {
            deleteTemplate(it.template.dbId)
        }
    }
}
