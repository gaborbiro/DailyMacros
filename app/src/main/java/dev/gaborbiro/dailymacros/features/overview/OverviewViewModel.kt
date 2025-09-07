package dev.gaborbiro.dailymacros.features.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dev.gaborbiro.dailymacros.App
import dev.gaborbiro.dailymacros.features.common.RecordsUIMapper
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelBase
import dev.gaborbiro.dailymacros.features.common.workers.MacrosWorkRequest
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
) : ViewModel() {

    private val _viewState: MutableStateFlow<OverviewViewState> =
        MutableStateFlow(OverviewViewState())
    val viewState: StateFlow<OverviewViewState> = _viewState.asStateFlow()

    fun onSearchTermChanged(search: String?) {
        viewModelScope.launch {
            repository.getFlowBySearchTerm(search)
                .map {
                    uiMapper.map(it, showDay = false)
                }
                .collect { records: List<ListUIModelBase> ->
                    _viewState.update {
                        if (records.isNotEmpty()) {
                            it.copy(records)
                        } else {
                            it.copy(showAddWidgetButton = true)
                        }
                    }
                }
        }
    }

    fun onRepeatMenuItemTapped(id: Long) {
        viewModelScope.launch {
            repository.duplicateRecord(id)
        }
        NotesWidget.reload()
    }

//    fun onChangeImageMenuItemTapped(record: RecordUIModel) {
//        if (record.hasMacros) {
//            _viewState.update {
//                it.copy(dialog = DialogState.ConfirmDestructiveChangeDialog(editState = EditState.ChangeImage(record.recordId)))
//            }
//        } else {
//            updateRecordPhoto(record.recordId)
//        }
//    }

//    fun onDeleteImageMenuItemTapped(record: RecordUIModel) {
//        if (record.bitmap != null) {
//            if (record.hasMacros) {
//                _viewState.update {
//                    it.copy(dialog = DialogState.ConfirmDestructiveChangeDialog(editState = EditState.RemoveImage(record.templateId)))
//                }
//            } else {
//                removePhoto(record.templateId)
//            }
//        }
//    }

    fun onDetailsMenuItemTapped(id: Long) {
        navigator.editRecord(recordId = id)
    }

    fun onDeleteRecordMenuItemTapped(id: Long) {
        viewModelScope.launch {
            val oldRecord = repository.deleteRecord(recordId = id)
            _viewState.update {
                it.copy(
                    showUndoDeleteSnackbar = true,
                    recordToUndelete = oldRecord,
                )
            }
            NotesWidget.reload()
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

    fun onMacrosMenuItemTapped(id: Long) {
        viewModelScope.launch {
            WorkManager.getInstance(App.appContext).enqueue(
                MacrosWorkRequest.getWorkRequest(
                    recordId = id
                )
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
