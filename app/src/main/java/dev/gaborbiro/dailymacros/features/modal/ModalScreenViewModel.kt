package dev.gaborbiro.dailymacros.features.modal

import android.net.Uri
import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.data.image.ImageStore
import dev.gaborbiro.dailymacros.features.common.DeleteRecordUseCase
import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.common.error.model.ErrorViewState
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.features.modal.model.ModalViewState
import dev.gaborbiro.dailymacros.features.modal.model.ImagePickerState
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.EditImageValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.EditRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.FetchMacrosUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodPicSummaryUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.features.widget.NotesWidget
import dev.gaborbiro.dailymacros.repo.chatgpt.model.DomainError
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import ellipsize
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ModalScreenViewModel(
    private val imageStore: ImageStore,
    private val recordsRepository: RecordsRepository,
    private val createRecordUseCase: CreateRecordUseCase,
    private val fetchMacrosUseCase: FetchMacrosUseCase,
    private val editRecordUseCase: EditRecordUseCase,
    private val editTemplateUseCase: EditTemplateUseCase,
    private val validateEditRecordUseCase: ValidateEditRecordUseCase,
    private val validateCreateRecordUseCase: ValidateCreateRecordUseCase,
    private val saveImageUseCase: SaveImageUseCase,
    private val validateEditImageUseCase: ValidateEditImageUseCase,
    private val editRecordImageUseCase: EditRecordImageUseCase,
    private val editTemplateImageUseCase: EditTemplateImageUseCase,
    private val getRecordImageUseCase: GetRecordImageUseCase,
    private val getTemplateImageUseCase: GetTemplateImageUseCase,
    private val foodPicSummaryUseCase: FoodPicSummaryUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase,
    private val macrosUIMapper: MacrosUIMapper,
) : ViewModel() {

    companion object {
        enum class EditTarget {
            RECORD, TEMPLATE
        }
    }

    private val _viewState: MutableStateFlow<ModalViewState> = MutableStateFlow(ModalViewState())
    val viewState: StateFlow<ModalViewState> = _viewState.asStateFlow()

    private val _errorState: MutableStateFlow<ErrorViewState?> = MutableStateFlow(null)
    val errorState: StateFlow<ErrorViewState?> = _errorState.asStateFlow()


    private var imageSummaryJob: Job? = null

    @UiThread
    fun addRecordWithCamera() {
        _viewState.update {
            it.copy(
                imagePicker = ImagePickerState.Take,
            )
        }
    }

    @UiThread
    fun addRecordWithImagePicker() {
        _viewState.update {
            it.copy(
                imagePicker = ImagePickerState.Select,
            )
        }
    }

    @UiThread
    fun addRecord() {
        _viewState.update {
            it.copy(
                dialog = DialogState.InputDialog.CreateDialog(),
            )
        }
    }

    @UiThread
    fun changeImage(recordId: Long) {
        _viewState.update {
            it.copy(
                imagePicker = ImagePickerState.ChangeImage(recordId),
            )
        }
    }

    fun viewRecordImage(recordId: Long) {
        runSafely {
            _viewState.update {
                it.copy(
                    dialog = getRecordImageUseCase.execute(recordId, thumbnail = false)!!
                )
            }
        }
    }

    fun viewTemplateImage(templateId: Long) {
        runSafely {
            _viewState.update {
                it.copy(
                    dialog = getTemplateImageUseCase.execute(templateId, thumbnail = false)!!
                )
            }
        }
    }

//    @UiThread
//    fun deleteRecord(recordId: Long) {
//        runSafely {
//            val record = repository.deleteRecord(recordId)
//            val (templateDeleted, imageDeleted) = repository.deleteTemplateIfUnused(record.template.id)
//            Log.d("Notes", "template deleted: $templateDeleted, image deleted: $imageDeleted")
//            refreshWidgetAndClose()
//        }
//    }

    @UiThread
    fun viewRecordDetails(recordId: Long) {
        runSafely {
            val record = recordsRepository.getRecord(recordId)!!
            _viewState.update {
                it.copy(
                    dialog = DialogState.InputDialog.RecordDetailsDialog(
                        recordId = recordId,
                        image = record.template.primaryImage,
                        title = record.template.name,
                        description = record.template.description,
                        calories = macrosUIMapper.mapCalories(value = record.template.macros?.calories),
                        protein = macrosUIMapper.mapProtein(value = record.template.macros?.protein),
                        carbs = macrosUIMapper.mapCarbohydrates(value = record.template.macros?.carbohydrates, sugar = null),
                        ofWhichSugar = macrosUIMapper.mapSugar(value = record.template.macros?.ofWhichSugar),
                        fat = macrosUIMapper.mapFat(value = record.template.macros?.fat, saturated = null),
                        ofWhichSaturated = macrosUIMapper.mapSaturated(value = record.template.macros?.ofWhichSaturated),
                        salt = macrosUIMapper.mapSalt(value = record.template.macros?.salt),
                        fibre = macrosUIMapper.mapFibre(value = record.template.macros?.fibre),
                        titleSuggestions = emptyList(),
                        validationError = null,
                        notes = record.template.macros?.notes,
                    ),
                )
            }
        }
    }

    @UiThread
    fun selectRecordAction(recordId: Long) {
        runSafely {
            _viewState.update {
                it.copy(
                    dialog = DialogState.SelectRecordActionDialog(recordId)
                )
            }
        }
    }

    @UiThread
    fun selectTemplateAction(templateId: Long) {
        runSafely {
            _viewState.update {
                it.copy(
                    dialog = DialogState.SelectTemplateActionDialog(templateId)
                )
            }
        }
    }

    @UiThread
    fun onImageAvailable(uri: Uri) {
        imageSummaryJob?.cancel()
        imageSummaryJob = runSafely {
            val imagePicker = _viewState.value.imagePicker!!
            _viewState.update {
                it.copy(
                    imagePicker = null,
                    dialog = DialogState.InputDialog.CreateWithImageDialog(
                        image = null,
                        showProgressIndicator = true,
                        suggestions = null,
                    ),
                )
            }
            val persistedFilename = saveImageUseCase.execute(uri)
            when (imagePicker) {
                ImagePickerState.Take, ImagePickerState.Select -> {
                    _viewState.update {
                        it.copy(
                            imagePicker = null,
                            dialog = DialogState.InputDialog.CreateWithImageDialog(
                                image = persistedFilename,
                                showProgressIndicator = true,
                                suggestions = null,
                            ),
                        )
                    }
                    try {
                        val summary = foodPicSummaryUseCase.execute(persistedFilename)
                        _viewState.update { currentState ->
                            if (currentState.dialog is DialogState.InputDialog.CreateWithImageDialog) {
                                currentState.copy(dialog = currentState.dialog.copy(suggestions = summary))
                            } else {
                                currentState
                            }
                        }
                    } catch (e: DomainError) {
                        e.printStackTrace()
                    } finally {
                        _viewState.update { currentState ->
                            if (currentState.dialog is DialogState.InputDialog.CreateWithImageDialog) {
                                currentState.copy(dialog = currentState.dialog.copy(showProgressIndicator = false))
                            } else {
                                currentState
                            }
                        }
                    }
                }

                is ImagePickerState.ChangeImage -> {
                    val result = validateEditImageUseCase.execute(imagePicker.recordId)
                    when (result) {
                        is EditImageValidationResult.AskConfirmation -> {
                            _viewState.update {
                                it.copy(
                                    imagePicker = null,
                                    dialog = DialogState.EditImageTargetConfirmationDialog(
                                        recordId = imagePicker.recordId,
                                        count = result.count,
                                        image = persistedFilename,
                                    ),
                                )
                            }
                        }

                        EditImageValidationResult.Valid -> {
                            NotesWidget.reload()
                            close()
                            editRecordImageUseCase.execute(imagePicker.recordId, persistedFilename)
                        }
                    }
                }
            }
        }
    }

    fun onImagePickerCanceled() {
        _viewState.update {
            it.copy(
                imagePicker = null,
                closeScreen = true,
            )
        }
    }

    @UiThread
    fun onRepeatRecordTapped(recordId: Long) {
        viewModelScope.launch {
            recordsRepository.duplicateRecord(recordId)
            NotesWidget.reload()
            close()
        }
    }

    @UiThread
    fun onRepeatTemplateTapped(templateId: Long) {
        viewModelScope.launch {
            recordsRepository.applyTemplate(templateId)
            NotesWidget.reload()
            close()
        }
    }

    @UiThread
    fun onEditTapped(recordId: Long) {
        viewRecordDetails(recordId)
    }

    @UiThread
    fun onDeleteTapped(recordId: Long) {
        viewModelScope.launch {
            deleteRecordUseCase.execute(recordId)
            NotesWidget.reload()
            close()
        }
    }

    @UiThread
    fun onDialogDismissRequested() {
        imageSummaryJob?.cancel()
        runSafely {
            _viewState.update {
                (it.dialog as? DialogState.InputDialog.CreateWithImageDialog)
                    ?.image?.let {
                        imageStore.delete(it)
                    }
                it.copy(
                    closeScreen = true,
                )
            }
        }
    }

    @UiThread
    fun onDestructiveChangeConfirmed() {
        val dialogState = (_viewState.value.dialog as? DialogState.ConfirmDestructiveChangeDialog)
        dialogState?.let {
            viewModelScope.launch {
                editRecordUseCase.execute(
                    recordId = dialogState.recordId,
                    title = dialogState.newTitle,
                    description = dialogState.newDescription,
                )
                NotesWidget.reload()
                close()
            }
        }
    }

    @UiThread
    fun onRecordDetailsUserTyping(title: String, description: String) {
        _viewState.update {
            it.copy(
                dialog = (it.dialog as? DialogState.InputDialog)
                    ?.withValidationError(validationError = null)
                    ?: it.dialog,
            )
        }
    }

    @UiThread
    fun onRecordDetailsSubmitRequested(title: String, description: String) {
        val dialogState = (_viewState.value.dialog as? DialogState.InputDialog)
        runSafely {
            when (dialogState) {
                is DialogState.InputDialog.CreateDialog, is DialogState.InputDialog.CreateWithImageDialog -> {
                    handleCreateRecordDetailsSubmitted(dialogState, title, description)
                }

                is DialogState.InputDialog.RecordDetailsDialog -> {
                    handleEditRecordDialogSubmitted(dialogState, title, description)
                }

                else -> {}
            }
        }
    }

    private suspend fun handleCreateRecordDetailsSubmitted(
        dialogState: DialogState.InputDialog,
        title: String,
        description: String,
    ) {
        val image = (dialogState as? DialogState.InputDialog.CreateWithImageDialog)?.image
        val result = validateCreateRecordUseCase.execute(image, title, description)

        when (result) {
            is CreateValidationResult.Error -> {
                applyValidationError(result.message)
            }

            is CreateValidationResult.Valid -> {
                NotesWidget.reload()
                close()
                val recordId = createRecordUseCase.execute(image, title, description)
                fetchMacrosUseCase.execute(recordId)
            }
        }
    }

    private suspend fun handleEditRecordDialogSubmitted(
        dialogState: DialogState.InputDialog.RecordDetailsDialog,
        title: String,
        description: String,
    ) {
        val result = validateEditRecordUseCase.execute(
            dialogState.recordId,
            title,
            description
        )
        when (result) {
            is EditValidationResult.ConfirmMultipleEdit -> {
                _viewState.update {
                    it.copy(
                        dialog = DialogState.EditTargetConfirmationDialog(
                            recordId = dialogState.recordId,
                            count = result.count,
                            newTitle = title,
                            newDescription = description,
                        ),
                    )
                }
            }

            is EditValidationResult.Valid -> {
                if (result.showMacrosDeletionConfirmationDialog) {
                    _viewState.update {
                        it.copy(
                            dialog = DialogState.ConfirmDestructiveChangeDialog(
                                recordId = dialogState.recordId,
                                newTitle = title,
                                newDescription = description,
                            ),
                        )
                    }
                } else {
                    editRecordUseCase.execute(
                        recordId = dialogState.recordId,
                        title = title,
                        description = description,
                    )
                    NotesWidget.reload()
                    close()
                }
            }

            is EditValidationResult.Error -> {
                applyValidationError(result.message)
            }
        }
    }

    private fun applyValidationError(message: String?) {
        _viewState.update {
            it.copy(
                dialog = (it.dialog as? DialogState.InputDialog)
                    ?.withValidationError(validationError = message)
                    ?: it.dialog,
            )
        }
    }

    @UiThread
    fun onEditImageTargetConfirmed(target: EditTarget) {
        NotesWidget.reload()
        close()
        (_viewState.value.dialog as? DialogState.EditImageTargetConfirmationDialog)?.let {
            runSafely {
                when (target) {
                    EditTarget.RECORD -> {
                        editRecordImageUseCase.execute(it.recordId, it.image)
                    }

                    EditTarget.TEMPLATE -> {
                        editTemplateImageUseCase.execute(it.recordId, it.image)
                    }
                }
            }
        }
    }

    @UiThread
    fun onEditTargetConfirmed(target: EditTarget) {
        NotesWidget.reload()
        close()
        (_viewState.value.dialog as? DialogState.EditTargetConfirmationDialog)?.let {
            val recordId = it.recordId
            val title = it.newTitle
            val description = it.newDescription
            runSafely {
                when (target) {
                    EditTarget.RECORD -> {
                        editRecordUseCase.execute(
                            recordId = recordId,
                            title = title,
                            description = description,
                        )
                    }

                    EditTarget.TEMPLATE -> {
                        editTemplateUseCase.execute(
                            recordId = recordId,
                            title = title,
                            description = description,
                        )
                    }
                }
            }
        }
    }

    fun onErrorDialogDismissRequested() {
        _errorState.update {
            null
        }
    }

    private fun close() {
        _viewState.update {
            it.copy(
                closeScreen = true,
                imagePicker = null,
            )
        }
    }

    private fun runSafely(task: suspend () -> Unit): Job {
        return viewModelScope.launch(errorHandler) {
            task()
        }
    }

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        _errorState.update {
            ErrorViewState(
                "Oops. Something went wrong ${
                    exception.message?.let {
                        "\n\n(${
                            it.ellipsize(
                                300
                            )
                        })"
                    } ?: ""
                }")
        }
        Log.w("BaseViewModel", "Uncaught exception", exception)
    }
}
