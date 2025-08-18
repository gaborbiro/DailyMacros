package dev.gaborbiro.dailymacros.features.overview

import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.common.RecordsUIMapper
import dev.gaborbiro.dailymacros.features.common.model.BaseListItemUIModel
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.modal.usecase.FetchMacrosUseCase
import dev.gaborbiro.dailymacros.features.overview.model.DialogState
import dev.gaborbiro.dailymacros.features.overview.model.EditState
import dev.gaborbiro.dailymacros.features.overview.model.OverviewViewState
import dev.gaborbiro.dailymacros.features.widget.NotesWidget
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import kotlinx.coroutines.GlobalScope
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
    private val fetchMacrosUseCase: FetchMacrosUseCase,
) : ViewModel() {

    private val _viewState: MutableStateFlow<OverviewViewState> =
        MutableStateFlow(OverviewViewState())
    val uiState: StateFlow<OverviewViewState> = _viewState.asStateFlow()

    fun onSearchTermChanged(search: String?) {
        viewModelScope.launch {
            repository.getFlowBySearchTerm(search)
                .map {
                    uiMapper.map(it, thumbnail = true)
                }
                .collect { records: List<BaseListItemUIModel> ->
                    _viewState.update {
                        it.copy(records)
                    }
                }
        }
    }

    fun onRepeatMenuItemTapped(record: RecordUIModel) {
        viewModelScope.launch {
            repository.duplicateRecord(record.recordId)
        }
        NotesWidget.reload()
    }

    fun onChangeImageMenuItemTapped(record: RecordUIModel) {
        if (record.hasMacros) {
            _viewState.update {
                it.copy(dialog = DialogState.ConfirmDestructiveChangeDialog(editState = EditState.ChangeImage(record.recordId)))
            }
        } else {
            updateRecordPhoto(record.recordId)
        }
    }

    fun onDeleteImageMenuItemTapped(record: RecordUIModel) {
        if (record.bitmap != null) {
            if (record.hasMacros) {
                _viewState.update {
                    it.copy(dialog = DialogState.ConfirmDestructiveChangeDialog(editState = EditState.RemoveImage(record.templateId)))
                }
            } else {
                removePhoto(record.templateId)
            }
        }
    }

    fun onDestructiveChangeConfirmed() {
        (_viewState.value.dialog as? DialogState.ConfirmDestructiveChangeDialog)?.let {
            when (it.editState) {
                is EditState.ChangeImage -> {
                    updateRecordPhoto(it.editState.recordId)
                }

                is EditState.RemoveImage -> {
                    removePhoto(it.editState.templateId)
                }
            }
            _viewState.update {
                it.copy(
                    dialog = null,
                )
            }
        }
    }

    private fun updateRecordPhoto(recordId: Long) {
        navigator.updateRecordPhoto(recordId)
    }

    private fun removePhoto(templateId: Long) {
        viewModelScope.launch {
            repository.deleteImage(templateId)
        }
        NotesWidget.reload()
    }

    @UiThread
    fun onDialogDismissRequested() {
        _viewState.update {
            it.copy(
                dialog = null,
            )
        }
    }

    fun onEditRecordMenuItemTapped(record: RecordUIModel) {
        navigator.editRecord(recordId = record.recordId)
    }

    fun onDeleteRecordMenuItemTapped(record: RecordUIModel) {
        viewModelScope.launch {
            val oldRecord = repository.deleteRecord(recordId = record.recordId)
            _viewState.update {
                it.copy(
                    showUndoDeleteSnackbar = true,
                    recordToUndelete = oldRecord,
                )
            }
            NotesWidget.reload()
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
            repository.updateRecord(_viewState.value.recordToUndelete!!)
        }
        _viewState.update {
            it.copy(
                recordToUndelete = null,
            )
        }
        NotesWidget.reload()
    }

    fun onUndoDeleteDismissed() {
        deleteTemplate(_viewState.value.recordToUndelete!!.template.dbId)
        _viewState.update {
            it.copy(
                recordToUndelete = null,
            )
        }
    }

    private fun deleteTemplate(templateId: Long) {
        GlobalScope.launch {
            val (templateDeleted, imageDeleted) = repository.deleteTemplateIfUnused(
                templateId = templateId,
                imageToo = true,
            )
            Log.d(
                "Notes",
                "template deleted: $templateDeleted, image deleted: $imageDeleted"
            )
            NotesWidget.reload()
        }
    }

    fun onMacrosMenuItemTapped(record: RecordUIModel) {
        viewModelScope.launch {
            fetchMacrosUseCase.execute(
                record.recordId
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
