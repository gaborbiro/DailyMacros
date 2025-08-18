package dev.gaborbiro.dailymacros.features.modal

import android.net.Uri
import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.DeleteRecordUseCase
import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.common.error.model.ErrorViewState
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.features.modal.model.ImagePickerState
import dev.gaborbiro.dailymacros.features.modal.model.ModalViewState
import dev.gaborbiro.dailymacros.features.modal.model.MacrosUIModel
import dev.gaborbiro.dailymacros.features.modal.usecase.AddRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.EditRecordUseCase
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
    private val addRecordImageUseCase: AddRecordImageUseCase,
    private val getRecordImageUseCase: GetRecordImageUseCase,
    private val getTemplateImageUseCase: GetTemplateImageUseCase,
    private val foodPicSummaryUseCase: FoodPicSummaryUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase,
    private val macrosUIMapper: MacrosUIMapper,
) : ViewModel() {

    companion object {
        enum class ChangeImagesTarget {
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
                dialogs = it.dialogs +
                        DialogState.NewImage(imagePickerState = ImagePickerState.Take(recordId = null)),
            )
        }
    }

    @UiThread
    fun addRecordWithImagePicker() {
        _viewState.update {
            it.copy(
                dialogs = it.dialogs +
                        DialogState.NewImage(imagePickerState = ImagePickerState.Select(recordId = null)),
            )
        }
    }

    @UiThread
    fun addRecord() {
        _viewState.update {
            it.copy(
                dialogs = it.dialogs + DialogState.InputDialog.CreateDialog(),
            )
        }
    }

    fun viewRecordImage(recordId: Long) {
        runSafely {
            _viewState.update {
                it.copy(
                    dialogs = it.dialogs + getRecordImageUseCase.execute(recordId, thumbnail = false)!!
                )
            }
        }
    }

    fun viewTemplateImage(templateId: Long) {
        runSafely {
            _viewState.update {
                it.copy(
                    dialogs = it.dialogs + getTemplateImageUseCase.execute(templateId, thumbnail = false)!!
                )
            }
        }
    }

    @UiThread
    fun viewRecordDetails(recordId: Long) {
        runSafely {
            val record = recordsRepository.getRecord(recordId)!!
            val macros = record.template.macros
                ?.let {
                    MacrosUIModel(
                        calories = macrosUIMapper.mapCalories(value = record.template.macros.calories),
                        protein = macrosUIMapper.mapProtein(value = record.template.macros.protein),
                        fat = macrosUIMapper.mapFat(value = record.template.macros.fat, saturated = null),
                        ofWhichSaturated = macrosUIMapper.mapSaturated(value = record.template.macros.ofWhichSaturated),
                        carbs = macrosUIMapper.mapCarbohydrates(value = record.template.macros.carbohydrates, sugar = null),
                        ofWhichSugar = macrosUIMapper.mapSugar(value = record.template.macros.ofWhichSugar),
                        salt = macrosUIMapper.mapSalt(value = record.template.macros.salt),
                        fibre = macrosUIMapper.mapFibre(value = record.template.macros.fibre),
                        notes = record.template.macros.notes,
                    )
                }
            val dialog = DialogState.InputDialog.RecordDetailsDialog(
                recordId = recordId,
                images = record.template.images,
                title = record.template.name,
                description = record.template.description,
                macros = macros,
                titleSuggestions = emptyList(),
                validationError = null,
            )
            _viewState.update {
                it.copy(
                    dialogs = listOf(dialog), // cancel any other dialogs
                )
            }
        }
    }

    @UiThread
    fun selectRecordAction(recordId: Long) {
        runSafely {
            _viewState.update {
                it.copy(
                    dialogs = it.dialogs + DialogState.SelectRecordActionDialog(recordId)
                )
            }
        }
    }

    @UiThread
    fun selectTemplateAction(templateId: Long) {
        runSafely {
            _viewState.update {
                it.copy(
                    dialogs = it.dialogs + DialogState.SelectTemplateActionDialog(templateId)
                )
            }
        }
    }

    @UiThread
    fun onImageSelected(uri: Uri, newImage: DialogState.NewImage) {
        imageSummaryJob?.cancel()
        imageSummaryJob = runSafely {
            var dialogs = _viewState.value.dialogs
            val persistedFilename = saveImageUseCase.execute(uri)

            dialogs = dialogs - newImage

            dialogs = dialogs.replaceInstances<DialogState.InputDialog.CreateWithImageDialog> {
                if (it.images.isEmpty()) {
                    fetchImageSummary(persistedFilename)
                }
                it.copy(
                    images = it.images + persistedFilename,
                    showProgressIndicator = it.images.isEmpty(),
                )
            }

            dialogs = dialogs.replaceInstances<DialogState.InputDialog.RecordDetailsDialog> {
                it.copy(
                    images = it.images + persistedFilename,
                )
            }

            if (dialogs.isEmpty()) {
                fetchImageSummary(persistedFilename)
                dialogs = listOf(
                    DialogState.InputDialog.CreateWithImageDialog(
                        images = listOf(persistedFilename),
                        showProgressIndicator = true,
                        suggestions = null,
                    )
                )
            }

            _viewState.update { currentState ->
                currentState.copy(
                    dialogs = dialogs
                )
            }

//            val persistedFilename = saveImageUseCase.execute(uri)
//            when (imagePicker) {
//                is ImagePickerState.Take, is ImagePickerState.Select -> {
//                    _viewState.update {
//                        it.copy(
//                            imagePicker = null,
//                            dialogs = it.dialogs.replaceInstances<DialogState.InputDialog.CreateWithImageDialog> {
//                                DialogState.InputDialog.CreateWithImageDialog(
//                                    image = persistedFilename,
//                                    showProgressIndicator = true,
//                                    suggestions = null,
//                                )
//                            }
//                        )
//                    }
//                    try {
//                        val summary = foodPicSummaryUseCase.execute(persistedFilename)
//                        _viewState.update { currentState ->
//                            currentState.copy(
//                                dialogs = currentState.dialogs.replaceInstances<DialogState.InputDialog.CreateWithImageDialog> {
//                                    it.copy(suggestions = summary)
//                                }
//                            )
//                        }
//                    } catch (e: DomainError) {
//                        e.printStackTrace()
//                    } finally {
//                        _viewState.update { currentState ->
//                            currentState.copy(
//                                dialogs = currentState.dialogs.replaceInstances<DialogState.InputDialog.CreateWithImageDialog> {
//                                    it.copy(showProgressIndicator = false)
//                                }
//                            )
//                        }
//                    }
//                }

//                is ImagePickerState.ChangeImage -> {
//                    val result = validateEditImageUseCase.execute(imagePicker.recordId)
//                    when (result) {
//                        is EditImageValidationResult.AskConfirmation -> {
//                            _viewState.update {
//                                it.copy(
//                                    imagePicker = null,
//                                    dialogs = it.dialogs +
//                                            DialogState.EditImageTargetConfirmationDialog(
//                                                recordId = imagePicker.recordId,
//                                                count = result.count,
//                                                image = persistedFilename,
//                                            ),
//                                )
//                            }
//                        }
//
//                        EditImageValidationResult.Valid -> {
//                            NotesWidget.reload()
//                            popScreen()
//                            editRecordImageUseCase.execute(imagePicker.recordId, persistedFilename)
//                        }
//                    }
//                }
//            }
        }
    }

    private fun fetchImageSummary(image: String) {
        viewModelScope.launch {
            try {
                val summary = foodPicSummaryUseCase.execute(image)
                _viewState.update { currentState ->
                    currentState.copy(
                        dialogs = currentState.dialogs.replaceInstances<DialogState.InputDialog.CreateWithImageDialog> {
                            it.copy(suggestions = summary)
                        }
                    )
                }
            } catch (e: DomainError) {
                e.printStackTrace()
            } finally {
                _viewState.update { currentState ->
                    currentState.copy(
                        dialogs = currentState.dialogs.replaceInstances<DialogState.InputDialog.CreateWithImageDialog> {
                            it.copy(showProgressIndicator = false)
                        }
                    )
                }
            }
        }
    }

    fun onNoImageSelected() {
        popDialog()
    }

    @UiThread
    fun onRepeatRecordTapped(recordId: Long) {
        viewModelScope.launch {
            recordsRepository.duplicateRecord(recordId)
            NotesWidget.reload()
            popDialog()
        }
    }

    @UiThread
    fun onRepeatTemplateTapped(templateId: Long) {
        viewModelScope.launch {
            recordsRepository.applyTemplate(templateId)
            NotesWidget.reload()
            popDialog()
        }
    }

    @UiThread
    fun onDetailsTapped(recordId: Long) {
        viewRecordDetails(recordId)
    }

    @UiThread
    fun onDeleteTapped(recordId: Long) {
        viewModelScope.launch {
            deleteRecordUseCase.execute(recordId)
            NotesWidget.reload()
            popDialog()
        }
    }

    @UiThread
    fun onDialogDismissRequested() {
        imageSummaryJob?.cancel()
        runSafely {
            _viewState.value.dialogs.filterIsInstance<DialogState.InputDialog.CreateWithImageDialog>()
                .forEach { dialog ->
                    dialog.images.forEach {
                        imageStore.delete(it)
                    }
                }
            popDialog()
        }
    }

    @UiThread
    fun onDeleteNutrientDataConfirmed() {
        _viewState.value.dialogs
            .filterIsInstance<DialogState.ConfirmDeleteNutrientDataDialog>()
            .firstOrNull()
            ?.let { dialogState ->
                viewModelScope.launch {
                    editRecordUseCase.execute(
                        recordId = dialogState.recordId,
                        images = dialogState.images,
                        title = dialogState.title,
                        description = dialogState.description,
                    )
                    NotesWidget.reload()
                    popDialog()
                }
            }
    }

    @UiThread
    fun onRecordDetailsUserTyping(title: String, description: String) {
        _viewState.update {
            it.copy(
                dialogs = it.dialogs.replaceInstances<DialogState.InputDialog> {
                    it.withValidationError(validationError = null)
                }
            )
        }
    }

    @UiThread
    fun onImageTapped(image: String) {
        viewModelScope.launch {
            imageStore.read(image, thumbnail = false)
                ?.let { bitmap ->
                    _viewState.update {
                        it.copy(
                            dialogs = it.dialogs + DialogState.ViewImageDialog("", bitmap)
                        )
                    }
                }
        }
    }

    @UiThread
    fun onAddImageViaCameraTapped(dialogState: DialogState.InputDialog) {
        when (dialogState) {
            is DialogState.InputDialog.CreateDialog, is DialogState.InputDialog.CreateWithImageDialog -> {
                _viewState.update {
                    it.copy(
                        dialogs = it.dialogs +
                                DialogState.NewImage(imagePickerState = ImagePickerState.Take(recordId = null)),
                    )
                }
            }

            is DialogState.InputDialog.RecordDetailsDialog -> {
                _viewState.update {
                    it.copy(
                        dialogs = it.dialogs +
                                DialogState.NewImage(imagePickerState = ImagePickerState.Take(recordId = dialogState.recordId)),
                    )
                }
            }
        }
    }

    @UiThread
    fun onAddImageViaPickerTapped(dialogState: DialogState.InputDialog) {
        when (dialogState) {
            is DialogState.InputDialog.CreateDialog, is DialogState.InputDialog.CreateWithImageDialog -> {
                _viewState.update {
                    it.copy(
                        dialogs = it.dialogs +
                                DialogState.NewImage(imagePickerState = ImagePickerState.Select(recordId = null)),
                    )
                }
            }

            is DialogState.InputDialog.RecordDetailsDialog -> {
                _viewState.update {
                    it.copy(
                        dialogs = it.dialogs +
                                DialogState.NewImage(imagePickerState = ImagePickerState.Select(recordId = dialogState.recordId)),
                    )
                }
            }
        }
    }

    @UiThread
    fun onRecordDetailsSubmitRequested(title: String, description: String) {
        _viewState.value.dialogs
            .filterIsInstance<DialogState.InputDialog>()
            .firstOrNull()
            ?.let {
                runSafely {
                    when (it) {
                        is DialogState.InputDialog.CreateDialog, is DialogState.InputDialog.CreateWithImageDialog -> {
                            handleCreateRecordDetailsSubmitted(it, title, description)
                        }

                        is DialogState.InputDialog.RecordDetailsDialog -> {
                            handleEditRecordDialogSubmitted(it, title, description)
                        }
                    }
                }
            }
    }

    private suspend fun handleCreateRecordDetailsSubmitted(
        dialogState: DialogState.InputDialog,
        title: String,
        description: String,
    ) {
        val images = (dialogState as? DialogState.InputDialog.CreateWithImageDialog)?.images
        val result =
            validateCreateRecordUseCase.execute(images ?: emptyList(), title, description)

        when (result) {
            is CreateValidationResult.Error -> {
                applyValidationError(result.message)
            }

            is CreateValidationResult.Valid -> {
                popDialog()
                val recordId =
                    createRecordUseCase.execute(images ?: emptyList(), title, description)
                fetchMacrosUseCase.execute(recordId)
                NotesWidget.reload()
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
                        dialogs = it.dialogs +
                                DialogState.EditTargetConfirmationDialog(
                                    recordId = dialogState.recordId,
                                    images = dialogState.images,
                                    count = result.count,
                                    title = title,
                                    description = description,
                                ),
                    )
                }
            }

            is EditValidationResult.Valid -> {
                if (result.showMacrosDeletionConfirmationDialog) {
                    _viewState.update {
                        it.copy(
                            dialogs = it.dialogs +
                                    DialogState.ConfirmDeleteNutrientDataDialog(
                                        recordId = dialogState.recordId,
                                        images = dialogState.images,
                                        title = title,
                                        description = description,
                                    ),
                        )
                    }
                } else {
                    editRecordUseCase.execute(
                        recordId = dialogState.recordId,
                        images = dialogState.images,
                        title = title,
                        description = description,
                    )
                    NotesWidget.reload()
                    popDialog()
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
                dialogs = it.dialogs.replaceInstances<DialogState.InputDialog> {
                    it.withValidationError(validationError = null)
                }
            )
        }
    }

    @UiThread
    fun onEditTargetConfirmed(target: ChangeImagesTarget) {
        _viewState.value.dialogs
            .filterIsInstance<DialogState.EditTargetConfirmationDialog>()
            .firstOrNull()
            ?.let {
                val recordId = it.recordId
                val images = it.images
                val title = it.title
                val description = it.description
                runSafely {
                    when (target) {
                        ChangeImagesTarget.RECORD -> {
                            editRecordUseCase.execute(
                                recordId = recordId,
                                images = images,
                                title = title,
                                description = description,
                            )
                        }

                        ChangeImagesTarget.TEMPLATE -> {
                            editTemplateUseCase.execute(
                                recordId = recordId,
                                images = images,
                                title = title,
                                description = description,
                            )
                        }
                    }
                }
            }
        NotesWidget.reload()
        popDialog()
    }

    fun onErrorDialogDismissRequested() {
        _errorState.update {
            null
        }
    }

    private fun popDialog() {
        _viewState.update {
            it.copy(
                dialogs = it.dialogs.dropLast(1),
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

    inline fun <reified R : DialogState> List<DialogState>.replaceInstances(
        noinline provider: (R) -> DialogState,
    ): List<DialogState> = map { e -> if (e is R) provider(e) else e }

}
