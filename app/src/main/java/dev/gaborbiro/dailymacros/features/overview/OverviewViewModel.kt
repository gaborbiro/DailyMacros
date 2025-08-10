package dev.gaborbiro.dailymacros.features.overview

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.features.common.ErrorViewModel
import dev.gaborbiro.dailymacros.features.common.RecordsUIMapper
import dev.gaborbiro.dailymacros.features.common.model.RecordViewState
import dev.gaborbiro.dailymacros.features.modal.usecase.FetchNutrientsUseCase
import dev.gaborbiro.dailymacros.features.overview.model.OverviewViewState
import dev.gaborbiro.dailymacros.features.overview.useCases.ObserveMacroGoalsUseCase
import dev.gaborbiro.dailymacros.store.bitmap.BitmapStore
import dev.gaborbiro.dailymacros.store.file.FileStoreFactoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OverviewViewModel(
    private val appContext: Context,
    private val navigator: OverviewNavigator,
    private val fetchNutrientsUseCase: FetchNutrientsUseCase,
    private val observeMacroGoalsUseCase: ObserveMacroGoalsUseCase,
) : ErrorViewModel() {

    private val fileStore by lazy { FileStoreFactoryImpl(appContext).getStore("public", keepFiles = true) }
    private val repository by lazy { RecordsRepository.get(fileStore) }
    private val uiMapper by lazy { RecordsUIMapper(BitmapStore(fileStore)) }

    private val _uiState: MutableStateFlow<OverviewViewState> =
        MutableStateFlow(OverviewViewState())
    val uiState: StateFlow<OverviewViewState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeMacroGoalsUseCase.execute()
                .collect { macroGoals ->
                    _uiState.update {
                        it.copy(macroGoals = macroGoals)
                    }
                }
        }
    }

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

    fun onRepeatMenuItemTapped(record: RecordViewState) {
        viewModelScope.launch {
            repository.duplicateRecord(record.recordId)
        }
        refreshWidget()
    }

    fun onChangeImageMenuItemTapped(record: RecordViewState) {
        navigator.updateRecordPhoto(record.recordId)
    }

    fun onDeleteImageMenuItemTapped(record: RecordViewState) {
        viewModelScope.launch {
            repository.deleteImage(record.templateId)
        }
        refreshWidget()
    }

    fun onEditRecordMenuItemTapped(record: RecordViewState) {
        navigator.editRecord(recordId = record.recordId)
    }

    fun onDeleteRecordMenuItemTapped(record: RecordViewState) {
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

    fun onRecordImageTapped(record: RecordViewState) {
        navigator.viewImage(record.recordId)
    }

    fun onRecordBodyTapped(record: RecordViewState) {
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

    fun onNutrientsMenuItemTapped(record: RecordViewState) {
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
