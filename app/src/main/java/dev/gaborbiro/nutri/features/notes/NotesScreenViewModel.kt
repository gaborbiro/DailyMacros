package dev.gaborbiro.nutri.features.notes

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.nutri.data.records.domain.RecordsRepository
import dev.gaborbiro.nutri.features.common.ErrorViewModel
import dev.gaborbiro.nutri.features.common.RecordsUIMapper
import dev.gaborbiro.nutri.features.common.model.RecordViewState
import dev.gaborbiro.nutri.features.modal.usecase.FetchNutrientsUseCase
import dev.gaborbiro.nutri.features.notes.model.NotesViewState
import dev.gaborbiro.nutri.store.bitmap.BitmapStore
import dev.gaborbiro.nutri.store.file.FileStoreFactoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotesScreenViewModel(
    private val appContext: Context,
    private val navigator: NotesListNavigator,
    private val fetchNutrientsUseCase: FetchNutrientsUseCase,
) : ErrorViewModel() {

    private val fileStore by lazy { FileStoreFactoryImpl(appContext).getStore("public", keepFiles = true) }
    private val repository by lazy { RecordsRepository.get(fileStore) }
    private val uiMapper by lazy { RecordsUIMapper(BitmapStore(fileStore)) }

    private val _uiState: MutableStateFlow<NotesViewState> = MutableStateFlow(NotesViewState())
    val uiState: StateFlow<NotesViewState> = _uiState.asStateFlow()

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

    fun onDuplicateRecordRequested(record: RecordViewState) {
        viewModelScope.launch {
            repository.duplicateRecord(record.recordId)
        }
        refreshWidget()
    }

    fun onUpdateImageRequested(record: RecordViewState) {
        navigator.updateRecordPhoto(record.recordId)
    }

    fun onDeleteImageRequested(record: RecordViewState) {
        viewModelScope.launch {
            repository.deleteImage(record.templateId)
        }
        refreshWidget()
    }

    fun onEditRecordRequested(record: RecordViewState) {
        navigator.editRecord(recordId = record.recordId)
    }

    fun onDeleteRecordRequested(record: RecordViewState) {
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

    fun onImageTapped(record: RecordViewState) {
        navigator.viewImage(record.recordId)
    }

    fun onUndoDeleteRequested() {
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

    fun onNutrientsRequested(record: RecordViewState) {
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
