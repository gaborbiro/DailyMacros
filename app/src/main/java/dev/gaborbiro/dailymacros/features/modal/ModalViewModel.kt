package dev.gaborbiro.dailymacros.features.modal

import android.net.Uri
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dev.gaborbiro.dailymacros.App
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.common.DeleteRecordUseCase
import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.common.workers.MacrosWorkRequest
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.features.modal.model.ImagePickerState
import dev.gaborbiro.dailymacros.features.modal.model.MacrosUIModel
import dev.gaborbiro.dailymacros.features.modal.model.ModalViewState
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.EditRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodPicSummaryUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.features.widget.DailyMacrosWidgetScreen
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

internal class ModalViewModel(
    private val imageStore: ImageStore,
    private val recordsRepository: RecordsRepository,
    private val createRecordUseCase: CreateRecordUseCase,
    private val editRecordUseCase: EditRecordUseCase,
    private val editTemplateUseCase: EditTemplateUseCase,
    private val validateEditRecordUseCase: ValidateEditRecordUseCase,
    private val validateCreateRecordUseCase: ValidateCreateRecordUseCase,
    private val saveImageUseCase: SaveImageUseCase,
    private val getRecordImageUseCase: GetRecordImageUseCase,
    private val getTemplateImageUseCase: GetTemplateImageUseCase,
    private val foodPicSummaryUseCase: FoodPicSummaryUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase,
    private val macrosUIMapper: MacrosUIMapper,
    private val appPrefs: AppPrefs,
) : ViewModel() {

    companion object {
        enum class ChangeImagesTarget {
            RECORD, TEMPLATE
        }
    }

    private val _viewState: MutableStateFlow<ModalViewState> = MutableStateFlow(ModalViewState())
    val viewState: StateFlow<ModalViewState> = _viewState.asStateFlow()

    private var imageSummaryJob: Job? = null

    @UiThread
    fun onAddRecordWithCameraDeeplink() {
        pushDialog(
            DialogState.NewImage(imagePickerState = ImagePickerState.Take(recordId = null))
        )
    }

    @UiThread
    fun onAddRecordWithImagePickerDeeplink() {
        pushDialog(
            DialogState.NewImage(imagePickerState = ImagePickerState.Select(recordId = null))
        )
    }

    @UiThread
    fun onAddRecordJustTextDeeplink() {
        pushDialog(
            DialogState.InputDialog.CreateDialog(
                titleSelectTooltipEnabled = false,
                checkAIPhotoDescriptionTooltipEnabled = false,
            )
        )
    }

    fun viewRecordImageDeeplink(recordId: Long) {
        runSafely {
            getRecordImageUseCase.execute(recordId, thumbnail = false)
                ?.let { imageDialog ->
                    pushDialog(
                        imageDialog
                    )
                }
                ?: run {
                    _viewState.update { it.copy(close = true) }
                }
        }
    }

    fun onViewTemplateImageDeeplink(templateId: Long) {
        runSafely {
            getTemplateImageUseCase.execute(templateId, thumbnail = false)
                ?.let { imageDialog ->
                    pushDialog(
                        imageDialog
                    )
                }
                ?: run {
                    _viewState.update { it.copy(close = true) }
                }
        }
    }

    @UiThread
    fun onViewRecordDetailsDeeplink(recordId: Long) {
        viewRecordDetails(recordId)
    }

    private fun viewRecordDetails(recordId: Long) {
        runSafely {
            val record =
                recordsRepository.getRecord(recordId)
                    ?: throw DomainError.DisplayMessageToUser.Message("Record not found")
            val macros = record.template.macros
                ?.let {
                    MacrosUIModel(
                        calories = macrosUIMapper.mapCalories(value = record.template.macros.calories),
                        protein = macrosUIMapper.mapProtein(value = record.template.macros.protein),
                        fat = macrosUIMapper.mapFat(value = record.template.macros.fat, saturated = null),
                        ofWhichSaturated = macrosUIMapper.mapSaturated(value = record.template.macros.ofWhichSaturated),
                        carbs = macrosUIMapper.mapCarbs(value = record.template.macros.carbohydrates, sugar = null),
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
    fun onSelectRecordActionDeeplink(recordId: Long) {
        pushDialog(
            DialogState.SelectRecordActionDialog(recordId)
        )
    }

    @UiThread
    fun onSelectTemplateActionDeeplink(templateId: Long) {
        pushDialog(
            DialogState.SelectTemplateActionDialog(templateId)
        )
    }

    @UiThread
    fun onImageSelected(uri: Uri, newImageDialog: DialogState.NewImage) {
        imageSummaryJob?.cancel()
        imageSummaryJob = runSafely {
            var dialogs = _viewState.value.dialogs
            val persistedFilename = saveImageUseCase.execute(uri)

            dialogs = dialogs - newImageDialog

            dialogs = dialogs.replaceInstances<DialogState.InputDialog.CreateWithImageDialog> {
                if (it.images.isEmpty()) {
                    fetchImageSummary(persistedFilename)
                }
                it.copy(
                    images = it.images + persistedFilename,
                    showProgressIndicator = it.images.isEmpty(),
                    titleSelectTooltipEnabled = appPrefs.selectTitleTooltipEnabled,
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
                        titleSelectTooltipEnabled = false,
                        checkAIPhotoDescriptionTooltipEnabled = false,
                    )
                )
            }

            _viewState.update { currentState ->
                currentState.copy(
                    dialogs = dialogs
                )
            }
        }
    }

    private fun fetchImageSummary(image: String) {
        runSafely {
            try {
                val summary = foodPicSummaryUseCase.execute(image)
                updateDialogsOfType<DialogState.InputDialog.CreateWithImageDialog> {
                    it.copy(
                        suggestions = summary,
                        titleSelectTooltipEnabled = summary.titles.isNotEmpty() && appPrefs.selectTitleTooltipEnabled,
                        checkAIPhotoDescriptionTooltipEnabled = summary.description.isNullOrEmpty().not() && appPrefs.checkAIPhotoDescriptionTooltipEnabled,
                    )
                }
            } catch (e: DomainError) {
                throw e
            } finally {
                updateDialogsOfType<DialogState.InputDialog.CreateWithImageDialog> {
                    it.copy(showProgressIndicator = false)
                }
            }
        }
    }

    fun onNoImageSelected() {
        popDialog()
    }

    @UiThread
    fun onRepeatRecordTapped(recordId: Long) {
        runSafely {
            recordsRepository.duplicateRecord(recordId)
            DailyMacrosWidgetScreen.reload()
            popDialog()
        }
    }

    @UiThread
    fun onRepeatTemplateTapped(templateId: Long) {
        runSafely {
            recordsRepository.applyTemplate(templateId)
            DailyMacrosWidgetScreen.reload()
            popDialog()
        }
    }

    @UiThread
    fun onDetailsTapped(recordId: Long) {
        viewRecordDetails(recordId)
    }

    @UiThread
    fun onDeleteTapped(recordId: Long) {
        runSafely {
            deleteRecordUseCase.execute(recordId)
            DailyMacrosWidgetScreen.reload()
            popDialog()
        }
    }

    @UiThread
    fun onDialogDismissRequested(dialog: DialogState) {
        imageSummaryJob?.cancel()
        (dialog as? DialogState.InputDialog.CreateWithImageDialog)?.let {
            runSafely {
                dialog.images.forEach {
                    imageStore.delete(it)
                }
            }
        }
        popDialog()
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
                    DailyMacrosWidgetScreen.reload()
                    popDialog()
                    popDialog()
                }
            }
    }

    @UiThread
    fun onRecordDetailsUserTyping(title: String, description: String) {
        updateDialogsOfType<DialogState.InputDialog> {
            it.withValidationError(validationError = null)
        }
    }

    @UiThread
    fun onImageTapped(image: String) {
        runSafely {
            imageStore.read(image, thumbnail = false)
                ?.let { bitmap ->
                    pushDialog(
                        DialogState.ViewImageDialog("", bitmap)
                    )
                }
        }
    }

    @UiThread
    fun onAddImageViaCameraTapped(dialogState: DialogState.InputDialog) {
        when (dialogState) {
            is DialogState.InputDialog.CreateDialog, is DialogState.InputDialog.CreateWithImageDialog -> {
                pushDialog(
                    DialogState.NewImage(imagePickerState = ImagePickerState.Take(recordId = null))
                )
            }

            is DialogState.InputDialog.RecordDetailsDialog -> {
                pushDialog(
                    DialogState.NewImage(imagePickerState = ImagePickerState.Take(recordId = dialogState.recordId))
                )
            }
        }
    }

    @UiThread
    fun onAddImageViaPickerTapped(dialogState: DialogState.InputDialog) {
        when (dialogState) {
            is DialogState.InputDialog.CreateDialog, is DialogState.InputDialog.CreateWithImageDialog -> {
                pushDialog(
                    DialogState.NewImage(imagePickerState = ImagePickerState.Select(recordId = null))
                )
            }

            is DialogState.InputDialog.RecordDetailsDialog -> {
                pushDialog(
                    DialogState.NewImage(imagePickerState = ImagePickerState.Select(recordId = dialogState.recordId))
                )
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

    fun onTitleSelectTooltipDismissed() {
        appPrefs.selectTitleTooltipEnabled = false
        updateDialogsOfType<DialogState.InputDialog.CreateWithImageDialog> {
            it.copy(titleSelectTooltipEnabled = false)
        }
    }

    fun onCheckAIPhotoDescriptionTooltipDismissed() {
        appPrefs.checkAIPhotoDescriptionTooltipEnabled = false
        updateDialogsOfType<DialogState.InputDialog.CreateWithImageDialog> {
            it.copy(checkAIPhotoDescriptionTooltipEnabled = false)
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
                val recordId =
                    createRecordUseCase.execute(images ?: emptyList(), title, description)
                DailyMacrosWidgetScreen.reload()
                WorkManager.getInstance(App.appContext).enqueue(
                    MacrosWorkRequest.getWorkRequest(
                        recordId = recordId
                    )
                )
                popDialog()
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
                pushDialog(
                    DialogState.EditTargetConfirmationDialog(
                        recordId = dialogState.recordId,
                        images = dialogState.images,
                        count = result.count,
                        title = title,
                        description = description,
                    )
                )
            }

            is EditValidationResult.Valid -> {
                if (result.showMacrosDeletionConfirmationDialog) {
                    pushDialog(
                        DialogState.ConfirmDeleteNutrientDataDialog(
                            recordId = dialogState.recordId,
                            images = dialogState.images,
                            title = title,
                            description = description,
                        ),
                    )
                } else {
                    editRecordUseCase.execute(
                        recordId = dialogState.recordId,
                        images = dialogState.images,
                        title = title,
                        description = description,
                    )
                    popDialog()
                    DailyMacrosWidgetScreen.reload()
                }
            }

            is EditValidationResult.Error -> {
                applyValidationError(result.message)
            }
        }
    }

    private fun applyValidationError(message: String?) {
        updateDialogsOfType<DialogState.InputDialog> {
            it.withValidationError(validationError = message)
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
        DailyMacrosWidgetScreen.reload()
        popDialog()
    }

    private fun runSafely(task: suspend () -> Unit): Job {
        return viewModelScope.launch(errorHandler) {
            task()
        }
    }

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
        pushDialog(
            DialogState.ErrorDialog(
                exception.message?.let {
                    "\n\n(${
                        it.ellipsize(
                            300
                        )
                    })"
                } ?: ""
            )
        )
    }

    private fun pushDialog(dialog: DialogState) {
        _viewState.update {
            it.copy(
                dialogs = it.dialogs +
                        dialog
            )
        }
    }

    private fun popDialog() {
        _viewState.update {
            val newDialogs = it.dialogs.dropLast(1)
            it.copy(
                dialogs = newDialogs,
                close = newDialogs.isEmpty(),
            )
        }
    }

    private inline fun <reified T : DialogState> updateDialogsOfType(noinline provider: (T) -> DialogState) {
        _viewState.update {
            it.copy(
                dialogs = it.dialogs.replaceInstances<T>(provider)
            )
        }
    }

    private inline fun <reified T : DialogState> List<DialogState>.replaceInstances(
        noinline provider: (T) -> DialogState,
    ): List<DialogState> = map { e -> if (e is T) provider(e) else e }
}
