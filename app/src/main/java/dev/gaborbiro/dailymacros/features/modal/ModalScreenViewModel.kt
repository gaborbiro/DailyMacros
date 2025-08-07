package dev.gaborbiro.dailymacros.features.modal

import android.net.Uri
import androidx.annotation.UiThread
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.data.chatgpt.model.DomainError
import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.features.common.ErrorViewModel
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.features.modal.model.HostViewState
import dev.gaborbiro.dailymacros.features.modal.model.ImagePickerState
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.EditImageValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.EditRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.FetchNutrientsUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodPicSummaryUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ObserveMacrosUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.store.bitmap.BitmapStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ModalScreenViewModel(
    private val bitmapStore: BitmapStore,
    private val recordsRepository: RecordsRepository,
    private val createRecordUseCase: CreateRecordUseCase,
    private val fetchNutrientsUseCase: FetchNutrientsUseCase,
    private val editRecordUseCase: EditRecordUseCase,
    private val editTemplateUseCase: EditTemplateUseCase,
    private val validateEditRecordUseCase: ValidateEditRecordUseCase,
    private val validateCreateRecordUseCase: ValidateCreateRecordUseCase,
    private val saveImageUseCase: SaveImageUseCase,
    private val validateEditImageUseCase: ValidateEditImageUseCase,
    private val editRecordImageUseCase: EditRecordImageUseCase,
    private val editTemplateImageUseCase: EditTemplateImageUseCase,
    private val getRecordImageUseCase: GetRecordImageUseCase,
    private val foodPicSummaryUseCase: FoodPicSummaryUseCase,
    private val observeMacrosUseCase: ObserveMacrosUseCase,
) : ErrorViewModel() {

    companion object {
        enum class EditTarget {
            RECORD, TEMPLATE
        }
    }

    private val _viewState: MutableStateFlow<HostViewState> = MutableStateFlow(HostViewState())
    val viewState: StateFlow<HostViewState> = _viewState.asStateFlow()

    private var imageSummaryJob: Job? = null

    init {
        viewModelScope.launch {
            observeMacrosUseCase.execute()
        }
    }

    @UiThread
    fun onStartWithCamera() {
        _viewState.update {
            it.copy(
                showCamera = true,
            )
        }
    }

    @UiThread
    fun onStartWithImagePicker() {
        _viewState.update {
            it.copy(
                imagePicker = ImagePickerState.Create,
            )
        }
    }

    @UiThread
    fun onStartWithJustText() {
        _viewState.update {
            it.copy(
                dialog = DialogState.InputDialog.Create(),
            )
        }
    }

    @UiThread
    fun onStartWithRedoImage(recordId: Long) {
        _viewState.update {
            it.copy(
                imagePicker = ImagePickerState.EditImage(recordId),
            )
        }
    }

    fun onStartWithShowImage(recordId: Long) {
        runSafely {
            val image = getRecordImageUseCase.execute(recordId, thumbnail = false)!!
            _viewState.update {
                it.copy(
                    dialog = DialogState.ShowImageDialog(image)
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
    fun onStartWithEdit(recordId: Long) {
        runSafely {
            val record = recordsRepository.getRecord(recordId)!!
            _viewState.update {
                it.copy(
                    dialog = DialogState.InputDialog.Edit(
                        recordId = recordId,
                        image = record.template.image,
                        title = record.template.name,
                        description = record.template.description,
                        validationError = null,
                    ),
                )
            }
        }
    }

    @UiThread
    fun onPhotoTaken(filename: String) {
        imageSummaryJob?.cancel()
        imageSummaryJob = runSafely {
            _viewState.update {
                it.copy(
                    showCamera = false,
                    dialog = DialogState.InputDialog.CreateWithImage(
                        image = filename,
                        titleSuggestionProgressIndicator = true,
                        titleSuggestions = emptyList(),
                    ),
                )
            }
            try {
                val summary = foodPicSummaryUseCase.execute(filename)
                _viewState.update { currentState ->
                    if (currentState.dialog is DialogState.InputDialog.CreateWithImage) {
                        currentState.copy(dialog = currentState.dialog.copy(titleSuggestions = summary))
                    } else {
                        currentState
                    }
                }
            } catch (e: DomainError) {
                e.printStackTrace()
            } finally {
                _viewState.update { currentState ->
                    if (currentState.dialog is DialogState.InputDialog.CreateWithImage) {
                        currentState.copy(dialog = currentState.dialog.copy(titleSuggestionProgressIndicator = false))
                    } else {
                        currentState
                    }
                }
            }
        }
    }

    @UiThread
    fun onImagePicked(uri: Uri?) {
        imageSummaryJob?.cancel()
        imageSummaryJob = runSafely {
            val imagePicker = _viewState.value.imagePicker!!
            _viewState.update {
                it.copy(
                    imagePicker = null,
                    dialog = DialogState.InputDialog.CreateWithImage(
                        image = null,
                        titleSuggestionProgressIndicator = true,
                        titleSuggestions = emptyList(),
                    ),
                )
            }
            val persistedFilename = uri?.let { saveImageUseCase.execute(it) }
            when (imagePicker) {
                ImagePickerState.Create -> {
                    _viewState.update {
                        it.copy(
                            imagePicker = null,
                            dialog = DialogState.InputDialog.CreateWithImage(
                                image = persistedFilename,
                                titleSuggestionProgressIndicator = true,
                                titleSuggestions = emptyList(),
                            ),
                        )
                    }
                    persistedFilename?.let {
                        try {
                            val summary = foodPicSummaryUseCase.execute(persistedFilename)
                            summary?.let {
                                _viewState.update { currentState ->
                                    if (currentState.dialog is DialogState.InputDialog.CreateWithImage) {
                                        currentState.copy(dialog = currentState.dialog.copy(titleSuggestions = it))
                                    } else {
                                        currentState
                                    }
                                }
                            }
                        } catch (e: DomainError) {
                            e.printStackTrace()
                        } finally {
                            _viewState.update { currentState ->
                                if (currentState.dialog is DialogState.InputDialog.CreateWithImage) {
                                    currentState.copy(dialog = currentState.dialog.copy(titleSuggestionProgressIndicator = false))
                                } else {
                                    currentState
                                }
                            }
                        }
                    }
                }

                is ImagePickerState.EditImage -> {
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
                            refreshWidgetAndClose()
                            editRecordImageUseCase.execute(imagePicker.recordId, persistedFilename)
                        }
                    }
                }
            }
        }
    }

    @UiThread
    fun onDialogDismissed() {
        imageSummaryJob?.cancel()
        runSafely {
            _viewState.update {
                (it.dialog as? DialogState.InputDialog.CreateWithImage)
                    ?.image?.let {
                        bitmapStore.delete(it)
                    }
                it.copy(
                    closeScreen = true,
                )
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
                is DialogState.InputDialog.Create, is DialogState.InputDialog.CreateWithImage -> {
                    handleCreateRecordDetailsSubmitted(dialogState, title, description)
                }

                is DialogState.InputDialog.Edit -> {
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
        val image = (dialogState as? DialogState.InputDialog.CreateWithImage)?.image
        val result = validateCreateRecordUseCase.execute(image, title, description)

        when (result) {
            is CreateValidationResult.Error -> {
                applyValidationError(result.message)
            }

            is CreateValidationResult.Valid -> {
                refreshWidgetAndClose()
                val recordId = createRecordUseCase.execute(image, title, description)
                fetchNutrientsUseCase.execute(recordId)
            }
        }
    }

    private suspend fun handleEditRecordDialogSubmitted(
        dialogState: DialogState.InputDialog.Edit,
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
                editRecordUseCase.execute(
                    recordId = dialogState.recordId,
                    title = title,
                    description = description,
                )
                refreshWidgetAndClose()
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
        refreshWidgetAndClose()
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
        refreshWidgetAndClose()
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

    private fun refreshWidgetAndClose() {
        _viewState.update {
            it.copy(
                refreshWidget = true,
                closeScreen = true,
                imagePicker = null,
            )
        }
    }
}
