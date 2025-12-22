package dev.gaborbiro.dailymacros.features.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.App
import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.common.RecordsUIMapper
import dev.gaborbiro.dailymacros.features.common.RepeatRecordUseCase
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelBase
import dev.gaborbiro.dailymacros.features.common.workers.GetMacrosWorker
import dev.gaborbiro.dailymacros.features.overview.model.OverviewViewState
import dev.gaborbiro.dailymacros.features.widgetDiary.DiaryWidgetScreen
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.settings.SettingsRepository
import dev.gaborbiro.dailymacros.repo.settings.model.Targets
import dev.gaborbiro.dailymacros.util.combine
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

internal class OverviewViewModel(
    private val navigator: OverviewNavigator,
    private val recordsRepository: RecordsRepository,
    private val repeatRecordUseCase: RepeatRecordUseCase,
    private val recordsUIMapper: RecordsUIMapper,
    private val overviewUIMapper: OverviewUIMapper,
    private val settingsRepository: SettingsRepository,
    private val appPrefs: AppPrefs,
) : ViewModel() {

    private val _viewState: MutableStateFlow<OverviewViewState> =
        MutableStateFlow(OverviewViewState())
    val viewState: StateFlow<OverviewViewState> = _viewState.asStateFlow()

    fun onSearchTermChanged(search: String?) {
        viewModelScope.launch {
            recordsRepository.getFlowBySearchTerm(search)
                .combine(flowOf(settingsRepository.loadTargets()))
                .map { (records: List<Record>, targets: Targets) ->
                    if (search.isNullOrBlank()) {
                        overviewUIMapper.map(records, targets, showDay = false)
                    } else {
                        records
                            .map {
                                recordsUIMapper.map(it, forceDay = true)
                            }
                            .reversed()
                    }
                }
                .collect { records: List<ListUIModelBase> ->
                    _viewState.update {
                        if (records.isNotEmpty()) {
                            it.copy(
                                items = records,
                                showSettingsButton = search.isNullOrBlank(),
                            )
                        } else {
                            it.copy(
                                items = records,
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

    fun onRepeatRecordMenuItemTapped(id: Long) {
        viewModelScope.launch {
            repeatRecordUseCase.execute(recordId = id)
        }
        DiaryWidgetScreen.reload()
    }

    fun onCoachMarkDismissed() {
        _viewState.update {
            it.copy(
                showCoachMark = false
            )
        }
    }

    fun onDetailsMenuItemTapped(id: Long) {
        navigator.editRecord(recordId = id)
    }

    fun onDeleteRecordMenuItemTapped(id: Long) {
        viewModelScope.launch {
            val oldRecord = recordsRepository.deleteRecord(recordId = id)
            _viewState.update {
                it.copy(
                    showUndoDeleteSnackbar = true,
                    recordToUndelete = oldRecord,
                )
            }
            DiaryWidgetScreen.reload()
            GetMacrosWorker.cancelWorkRequest(
                appContext = App.appContext,
                recordId = id,
            )
        }
    }

    fun onRecordImageTapped(id: Long) {
        navigator.viewImage(id)
    }

    fun onRecordBodyTapped(id: Long) {
        navigator.editRecord(id)
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
        navigator.openSettingsScreen()
        _viewState.update {
            it.copy(
                showCoachMark = false
            )
        }
    }

    fun onTrendsButtonTapped() {
        navigator.openTrendsScreen()
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

    fun onMacrosMenuItemTapped(id: Long) {
        viewModelScope.launch {
            GetMacrosWorker.cancelWorkRequest(
                appContext = App.appContext,
                recordId = id,
            )
            GetMacrosWorker.setWorkRequest(
                appContext = App.appContext,
                recordId = id,
                force = true,
            )
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
