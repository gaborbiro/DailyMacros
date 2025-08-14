package dev.gaborbiro.dailymacros.features.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.features.common.RecordsUIMapper
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.modal.usecase.FetchNutrientsUseCase
import dev.gaborbiro.dailymacros.features.overview.model.OverviewViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class OverviewViewModel(
    private val navigator: OverviewNavigator,
    private val repository: RecordsRepository,
    private val uiMapper: RecordsUIMapper,
    private val fetchNutrientsUseCase: FetchNutrientsUseCase,
) : ViewModel() {

    private val _uiState: MutableStateFlow<OverviewViewState> =
        MutableStateFlow(OverviewViewState())
    val uiState: StateFlow<OverviewViewState> = _uiState.asStateFlow()

    fun onSearchTermChanged(search: String?) {
        viewModelScope.launch {
            repository.getFlowBySearchTerm(search)
                .map {
                    uiMapper.map(it, thumbnail = true)
                }
                .collect { records ->
                    _uiState.update {
                        it.copy(records)
                    }
                }
        }
    }

    fun onRepeatMenuItemTapped(record: RecordUIModel) {
        viewModelScope.launch {
            repository.duplicateRecord(record.recordId)
        }
        refreshWidget()
    }

    fun onChangeImageMenuItemTapped(record: RecordUIModel) {
        navigator.updateRecordPhoto(record.recordId)
    }

    fun onDeleteImageMenuItemTapped(record: RecordUIModel) {
        viewModelScope.launch {
            repository.deleteImage(record.templateId)
        }
        refreshWidget()
    }

    fun onEditRecordMenuItemTapped(record: RecordUIModel) {
        navigator.editRecord(recordId = record.recordId)
    }

    fun onDeleteRecordMenuItemTapped(record: RecordUIModel) {
        viewModelScope.launch {
            val oldRecord = repository.deleteRecord(recordId = record.recordId)
            _uiState.update {
                it.copy(
                    showUndoDeleteSnackbar = true,
                    recordToUndelete = oldRecord,
                )
            }
            refreshWidget()
        }
    }

    fun onRecordImageTapped(record: RecordUIModel) {
        navigator.viewImage(record.recordId)
    }

    fun onRecordBodyTapped(record: RecordUIModel) {
        navigator.editRecord(record.recordId)
    }

    fun onUndoDeleteTapped() {
        viewModelScope.launch {
            repository.updateRecord(_uiState.value.recordToUndelete!!)
        }
        _uiState.update {
            it.copy(
                recordToUndelete = null,
            )
        }
    }

    fun onUndoDeleteDismissed() {
        viewModelScope.launch {
            val (templateDeleted, imageDeleted) = repository.deleteTemplateIfUnused(
                _uiState.value.recordToUndelete!!.template.id
            )
            Log.d(
                "Notes",
                "template deleted: $templateDeleted, image deleted: $imageDeleted"
            )
        }
        _uiState.update {
            it.copy(
                recordToUndelete = null,
            )
        }
    }

    fun onNutrientsMenuItemTapped(record: RecordUIModel) {
        viewModelScope.launch {
            fetchNutrientsUseCase.execute(
                record.recordId
            )
        }
    }

    private fun refreshWidget() {
        _uiState.update {
            it.copy(refreshWidget = true)
        }
    }

    fun onWidgetRefreshed() {
        _uiState.update {
            it.copy(refreshWidget = false)
        }
    }

    fun onUndoDeleteSnackbarShown() {
        _uiState.update {
            it.copy(
                showUndoDeleteSnackbar = false
            )
        }
    }
}
