package dev.gaborbiro.dailymacros.features.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.dailymacros.features.common.utils.combine
import dev.gaborbiro.dailymacros.features.shared.NutrientAnalysisWorker
import dev.gaborbiro.dailymacros.features.modal.usecase.ApplyQuickPickOverrideAndReloadWidgetUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ListMealVariantsForTemplateUseCase
import dev.gaborbiro.dailymacros.features.overview.model.OverviewUiState
import dev.gaborbiro.dailymacros.features.overview.model.OverviewUiUpdates
import dev.gaborbiro.dailymacros.features.overview.usecase.CancelMacrosAnalysisForRecordUseCase
import dev.gaborbiro.dailymacros.features.overview.usecase.ComputeOverviewHasMoreItemsUseCase
import dev.gaborbiro.dailymacros.features.overview.usecase.DeleteUnusedTemplateIfOrphanedUseCase
import dev.gaborbiro.dailymacros.features.overview.usecase.ResolveOverviewCoachMarkUseCase
import dev.gaborbiro.dailymacros.features.overview.usecase.ResolveOverviewObserveSinceEpochMillisUseCase
import dev.gaborbiro.dailymacros.features.shared.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelBase
import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelRecord
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OverviewViewModel @Inject constructor(
    application: Application,
    private val recordsRepository: RecordsRepository,
    private val settingsRepository: SettingsRepository,
    private val uiMapper: OverviewUiMapper,
    private val resolveObserveSinceEpochMillis: ResolveOverviewObserveSinceEpochMillisUseCase,
    private val computeHasMoreItems: ComputeOverviewHasMoreItemsUseCase,
    private val resolveCoachMark: ResolveOverviewCoachMarkUseCase,
    private val deleteUnusedTemplateIfOrphaned: DeleteUnusedTemplateIfOrphanedUseCase,
    private val cancelMacrosAnalysisForRecord: CancelMacrosAnalysisForRecordUseCase,
    private val listMealVariantsForTemplateUseCase: ListMealVariantsForTemplateUseCase,
    private val createRecordFromTemplateUseCase: CreateRecordFromTemplateUseCase,
) : AndroidViewModel(application) {

    private val _viewState: MutableStateFlow<OverviewUiState> =
        MutableStateFlow(OverviewUiState())
    val viewState: StateFlow<OverviewUiState> = _viewState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<OverviewUiUpdates>()
    val uiUpdates: SharedFlow<OverviewUiUpdates> = _uiUpdates.asSharedFlow()

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
            val searchBlank = search.isNullOrBlank()
            val sinceMillis = resolveObserveSinceEpochMillis.execute(searchBlank, sinceEpochMillis)
            recordsRepository.observeRecords(search, sinceEpochMillis = sinceMillis)
                .combine(flowOf(settingsRepository.getTargets()))
                .mapLatest { (records: List<Record>, targets: Targets) ->
                    val items = if (searchBlank) {
                        uiMapper.map(records, targets)
                    } else {
                        uiMapper.mapSearchResults(records)
                    }
                    enrichRecordRowsWithOtherVariantsIcon(items)
                }
                .collect { records: List<ListUiModelBase> ->
                    val hasMore = computeHasMoreItems.execute(
                        isSearchActive = !searchBlank,
                        previousItemCount = previousRecordCount,
                        currentItemCount = records.size,
                    )
                    previousRecordCount = records.size

                    val notSearching = searchBlank
                    _viewState.update {
                        if (records.isNotEmpty()) {
                            it.copy(
                                items = records,
                                isLoadingMore = false,
                                hasMoreData = hasMore,
                                showSettingsButton = notSearching,
                                showTrendsButton = notSearching,
                            )
                        } else {
                            it.copy(
                                items = records,
                                isLoadingMore = false,
                                hasMoreData = hasMore,
                                showAddWidgetButton = notSearching,
                                showSettingsButton = notSearching,
                                showTrendsButton = notSearching,
                            )
                        }
                    }
                    if (resolveCoachMark.execute(records.size)) {
                        delay(2.seconds)
                        _viewState.update {
                            val stillNotSearching = search.isNullOrBlank()
                            it.copy(
                                showCoachMark = true,
                                showSettingsButton = stillNotSearching,
                                showTrendsButton = stillNotSearching,
                            )
                        }
                    }
                }
        }
    }

    private suspend fun enrichRecordRowsWithOtherVariantsIcon(items: List<ListUiModelBase>): List<ListUiModelBase> =
        items.map { item ->
            when (item) {
                is ListUiModelRecord -> {
                    val show = listMealVariantsForTemplateUseCase.hasOtherVariants(item.templateId)
                    item.copy(showOtherLoggedVariantsIcon = show)
                }
                else -> item
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
            val templateId = recordsRepository.get(recordId)?.template?.dbId ?: return@launch
            createRecordFromTemplateUseCase.execute(templateId)
        }
    }

    fun onAnalyseMacrosMenuItemTapped(recordId: Long) {
        viewModelScope.launch {
            NutrientAnalysisWorker.cancelWorkRequest(
                appContext = application,
                recordId = recordId,
            )
            NutrientAnalysisWorker.setWorkRequest(
                appContext = application,
                recordId = recordId,
                force = true,
            )
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
            cancelMacrosAnalysisForRecord.execute(recordId)
        }
    }

    fun onRecordImageTapped(recordId: Long) {
        viewModelScope.launch {
            _uiUpdates.emit(OverviewUiUpdates.ViewImage(recordId))
        }
    }

    fun onRecordBodyTapped(recordId: Long) {
        viewModelScope.launch {
            _uiUpdates.emit(OverviewUiUpdates.EditRecord(recordId))
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
    }

    fun onUndoDeleteDismissed() {
        deleteUnusedTemplateAfterUndo(_viewState.value.recordToUndelete!!.template.dbId)
        _viewState.update {
            it.copy(
                recordToUndelete = null,
            )
        }
    }

    fun onSettingsButtonTapped() {
        viewModelScope.launch {
            _uiUpdates.emit(OverviewUiUpdates.OpenSettingsScreen)
        }
        _viewState.update {
            it.copy(
                showCoachMark = false
            )
        }
    }

    fun onTrendsButtonTapped() {
        viewModelScope.launch {
            _uiUpdates.emit(OverviewUiUpdates.OpenTrendsScreen)
        }
    }

    private fun deleteUnusedTemplateAfterUndo(templateId: Long) {
        viewModelScope.launch {
            deleteUnusedTemplateIfOrphaned.execute(templateId)
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
            deleteUnusedTemplateAfterUndo(it.template.dbId)
        }
    }
}
