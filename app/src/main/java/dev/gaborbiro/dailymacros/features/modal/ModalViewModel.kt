package dev.gaborbiro.dailymacros.features.modal

import android.net.Uri
import androidx.annotation.UiThread
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.App
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.common.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.common.DeleteRecordUseCase
import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.common.RepeatRecordUseCase
import dev.gaborbiro.dailymacros.features.common.message
import dev.gaborbiro.dailymacros.features.common.workers.GetMacrosWorker
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.features.modal.model.ImageInputType
import dev.gaborbiro.dailymacros.features.modal.model.MacrosUIModel
import dev.gaborbiro.dailymacros.features.modal.model.ModalUIUpdates
import dev.gaborbiro.dailymacros.features.modal.model.ModalViewState
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.EditTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodPicSummaryUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.UpdateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.features.widgetDiary.DiaryWidgetScreen
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.DomainError
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import ellipsize
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ModalViewModel(
    private val imageStore: ImageStore,
    private val recordsRepository: RecordsRepository,
    private val createRecordFromTemplateUseCase: CreateRecordFromTemplateUseCase,
    private val createRecordWithNewTemplateUseCase: CreateRecordWithNewTemplateUseCase,
    private val updateRecordWithNewTemplateUseCase: UpdateRecordWithNewTemplateUseCase,
    private val editTemplateUseCase: EditTemplateUseCase,
    private val repeatRecordUseCase: RepeatRecordUseCase,
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

    private val _uiUpdates = Channel<ModalUIUpdates>(Channel.BUFFERED)
    val uiUpdates: Flow<ModalUIUpdates> = _uiUpdates.receiveAsFlow()

    private var imageSummaryJob: Job? = null

    @UiThread
    fun onCreateRecordWithCameraDeeplink() {
        pushDialog(
            DialogState.ImageInput(type = ImageInputType.TakePhoto)
        )
    }

    @UiThread
    fun onCreateRecordWithQuickCameraDeeplink() {
        pushDialog(
            DialogState.ImageInput(type = ImageInputType.QuickPhoto)
        )
    }

    @UiThread
    fun onCreateRecordWithImagePickerDeeplink() {
        pushDialog(
            DialogState.ImageInput(type = ImageInputType.Browse)
        )
    }

    @UiThread
    fun onCreateRecordDeeplink() {
        pushDialog(
            DialogState.InputDialog.CreateDialog(
                titleHint = "Describe your meal (or snap a photo)",
                title = TextFieldValue(),
                description = TextFieldValue(),
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
        runSafely { viewRecordDetails(recordId, edit = true) }
    }

    private fun viewRecordDetails(recordId: Long, edit: Boolean) {
        runSafely {
            recordsRepository.observe(recordId)
                .collect { record ->
                    val macros = record.template.macros
                        ?.let {
                            MacrosUIModel(
                                calories = macrosUIMapper.formatCalories(value = record.template.macros.calories, isShort = false, withLabel = true),
                                protein = macrosUIMapper.formatProtein(value = record.template.macros.protein, isShort = false, withLabel = true),
                                fat = macrosUIMapper.formatFat(value = record.template.macros.fat, saturated = null, isShort = false, withLabel = true),
                                ofWhichSaturated = macrosUIMapper.formatSaturatedFat(value = record.template.macros.ofWhichSaturated, isShort = false, withLabel = true),
                                carbs = macrosUIMapper.formatCarbs(value = record.template.macros.carbs, sugar = null, isShort = false, withLabel = true),
                                ofWhichSugar = macrosUIMapper.formatSugar(value = record.template.macros.ofWhichSugar, isShort = false, withLabel = true),
                                salt = macrosUIMapper.formatSalt(value = record.template.macros.salt, isShort = false, withLabel = true),
                                fibre = macrosUIMapper.formatFibre(value = record.template.macros.fibre, isShort = false, withLabel = true),
                                notes = record.template.macros.notes,
                            )
                        }
                    val dialog = DialogState.InputDialog.RecordDetailsDialog(
                        recordId = recordId,
                        images = record.template.images,
                        title = TextFieldValue(record.template.name),
                        description = TextFieldValue(record.template.description),
                        macros = macros,
                        allowEdit = edit,
                        titleSuggestions = emptyList(),
                        titleHint = "Describe your meal",
                        validationError = null,
                    )
                    _viewState.emit(
                        ModalViewState(
                            dialogs = listOf(dialog), // cancel any other dialogs
                        )
                    )
                }
        }
    }

    @UiThread
    fun onSelectRecordActionDeeplink(recordId: Long) {
        runSafely {
            val title = recordsRepository.get(recordId)?.template?.name ?: ""
            pushDialog(
                DialogState.SelectRecordActionDialog(recordId, title)
            )
        }
    }

    @UiThread
    fun onSelectTemplateActionDeeplink(templateId: Long) {
        runSafely {
            val title = recordsRepository.getRecordsByTemplate(templateId).firstOrNull()?.template?.name ?: ""
            pushDialog(
                DialogState.SelectTemplateActionDialog(templateId, title)
            )
        }
    }

    @UiThread
    fun onImageSelected(uri: Uri, imageInputDialog: DialogState.ImageInput) {
        imageSummaryJob?.cancel()
        imageSummaryJob = runSafely {
            var dialogs = _viewState.value.dialogs
            val persistedFilename = saveImageUseCase.execute(uri)

            dialogs = dialogs - imageInputDialog

            dialogs = dialogs.replaceInstances<DialogState.InputDialog.CreateWithImageDialog> {
                it.copy(
                    images = it.images + persistedFilename,
                    suggestions = null,
                    showProgressIndicator = true,
                )
            }

            dialogs = dialogs.replaceInstances<DialogState.InputDialog.CreateDialog> {
                DialogState.InputDialog.CreateWithImageDialog(
                    images = listOf(persistedFilename),
                    suggestions = null,
                    autoSubmitEnabled = appPrefs.autoSubmitEnabled,
                    showProgressIndicator = true,
                    titleHint = "Describe your meal (or tap one of the AI suggestions)",
                    title = it.title,
                    description = it.description,
                )
            }

            dialogs = dialogs.replaceInstances<DialogState.InputDialog.RecordDetailsDialog> {
                it.copy(
                    images = it.images + persistedFilename,
                )
            }

            if (dialogs.isEmpty()) {
                dialogs = listOf(
                    DialogState.InputDialog.CreateWithImageDialog(
                        images = listOf(persistedFilename),
                        suggestions = null,
                        autoSubmitEnabled = appPrefs.autoSubmitEnabled,
                        showProgressIndicator = true,
                        titleHint = "Describe your meal (or tap one of the AI suggestions)",
                        title = TextFieldValue(),
                        description = TextFieldValue(),
                    )
                )
            }

            dialogs.filterIsInstance<DialogState.InputDialog.CreateWithImageDialog>().firstOrNull()
                ?.let {
                    fetchSummary(it.images)
                }

            _viewState.emit(
                ModalViewState(
                    dialogs = dialogs, // cancel any other dialogs
                )
            )
        }
    }

    private fun fetchSummary(images: List<String>) {
        runSafely {
            try {
                val summary = foodPicSummaryUseCase.execute(images)
                updateDialogsOfType<DialogState.InputDialog.CreateWithImageDialog> {
                    val title = summary.titles.firstOrNull() ?: ""
                    it.copy(
                        suggestions = summary,
                        title = TextFieldValue(title, selection = TextRange((title.length - 1).fastCoerceAtLeast(0))),
                        showProgressIndicator = false,
                    )
                }
                if (appPrefs.autoSubmitEnabled) {
                    onSubmitRequested()
                    popDialog()
                }
            } catch (t: Throwable) {
                throw t
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
    fun onRepeatRecordButtonTapped(recordId: Long) {
        popDialog()
        runSafely {
            repeatRecordUseCase.execute(recordId)
            DiaryWidgetScreen.reload()
        }
    }

    @UiThread
    fun onRepeatTemplateButtonTapped(templateId: Long) {
        popDialog()
        runSafely {
            createRecordFromTemplateUseCase.execute(templateId)
            DiaryWidgetScreen.reload()
        }
    }

    @UiThread
    fun onRecordDetailsButtonTapped(recordId: Long) {
        runSafely { viewRecordDetails(recordId, edit = true) }
    }

    @UiThread
    fun onTemplateDetailsButtonTapped(templateId: Long) {
        runSafely {
            recordsRepository.getRecordsByTemplate(templateId).firstOrNull()
                ?.let {
                    viewRecordDetails(it.recordId, edit = false)
                }
        }
    }

    @UiThread
    fun onDeleteTapped(recordId: Long) {
        popDialog()
        runSafely {
            deleteRecordUseCase.execute(recordId)
            DiaryWidgetScreen.reload()
            GetMacrosWorker.cancelWorkRequest(
                appContext = App.appContext,
                recordId = recordId,
            )
        }
    }

    @UiThread
    fun onDialogDismissRequested(dialog: DialogState) {
        popDialog()
        imageSummaryJob?.cancel()
        (dialog as? DialogState.InputDialog.CreateWithImageDialog)?.let {
            runSafely {
                dialog.images.forEach {
                    imageStore.delete(it)
                }
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
                    DialogState.ImageInput(type = ImageInputType.TakePhoto)
                )
            }

            is DialogState.InputDialog.RecordDetailsDialog -> {
                pushDialog(
                    DialogState.ImageInput(type = ImageInputType.TakePhoto)
                )
            }
        }
    }

    @UiThread
    fun onAddImageViaPickerTapped(dialogState: DialogState.InputDialog) {
        when (dialogState) {
            is DialogState.InputDialog.CreateDialog, is DialogState.InputDialog.CreateWithImageDialog -> {
                pushDialog(
                    DialogState.ImageInput(type = ImageInputType.Browse)
                )
            }

            is DialogState.InputDialog.RecordDetailsDialog -> {
                pushDialog(
                    DialogState.ImageInput(type = ImageInputType.Browse)
                )
            }
        }
    }

    @UiThread
    fun onTitleSuggestionSelected(suggestion: String) {
        runSafely {
            var dialogs = _viewState.value.dialogs
            dialogs = dialogs.replaceInstances<DialogState.InputDialog.CreateWithImageDialog> {
                it.copy(
                    title = TextFieldValue(
                        text = suggestion,
                        selection = TextRange((suggestion.length - 1).coerceAtLeast(0))
                    )
                )
            }

            _viewState.emit(
                ModalViewState(
                    dialogs = dialogs,
                )
            )
        }
    }

    @UiThread
    fun onDescriptionSuggestionSelected(suggestion: String) {
        runSafely {
            var dialogs = _viewState.value.dialogs
            dialogs = dialogs.replaceInstances<DialogState.InputDialog.CreateWithImageDialog> {
                it.copy(
                    description = TextFieldValue(
                        text = suggestion,
                        selection = TextRange((suggestion.length - 1).coerceAtLeast(0))
                    )
                )
            }

            _viewState.emit(
                ModalViewState(
                    dialogs = dialogs,
                )
            )
        }
    }

    @UiThread
    fun onAutoSubmitCheckedChanged(checked: Boolean) {
        imageSummaryJob = runSafely {
            appPrefs.autoSubmitEnabled = checked
            var dialogs = _viewState.value.dialogs
            dialogs = dialogs.replaceInstances<DialogState.InputDialog.CreateWithImageDialog> {
                it.copy(
                    autoSubmitEnabled = checked
                )
            }

            _viewState.emit(
                ModalViewState(
                    dialogs = dialogs,
                )
            )
        }
    }

    @UiThread
    fun onSubmitRequested() {
        _viewState.value.dialogs
            .filterIsInstance<DialogState.InputDialog>()
            .firstOrNull()
            ?.let {
                runSafely {
                    when (it) {
                        is DialogState.InputDialog.CreateDialog, is DialogState.InputDialog.CreateWithImageDialog -> {
                            handleCreateRecordDetailsSubmitted(it, it.title.text.trim(), it.description.text.trim())
                        }

                        is DialogState.InputDialog.RecordDetailsDialog -> {
                            handleEditRecordDialogSubmitted(it, it.title.text.trim(), it.description.text.trim())
                        }
                    }
                }
            }
    }

    fun onImagesInfoButtonTapped() {
        pushDialog(DialogState.InfoDialog("You can add as many images as you like. Nutritional labels are particularly useful to the AI. You can also add more photos or update things later, don't worry about gathering all info right away."))
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
                val recordId = createRecordWithNewTemplateUseCase.execute(
                    images = images ?: emptyList(),
                    title = title,
                    description = description,
                )
                DiaryWidgetScreen.reload()
                GetMacrosWorker.setWorkRequest(
                    appContext = App.appContext,
                    recordId = recordId,
                    force = true,
                )
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
                popDialog()
                updateRecordWithNewTemplateUseCase.execute(
                    recordId = dialogState.recordId,
                    images = dialogState.images,
                    title = title,
                    description = description,
                )
                DiaryWidgetScreen.reload()
                GetMacrosWorker.setWorkRequest(
                    appContext = App.appContext,
                    recordId = dialogState.recordId,
                    force = true,
                )
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
                            updateRecordWithNewTemplateUseCase.execute(
                                recordId = recordId,
                                images = images,
                                title = title,
                                description = description,
                            )
                        }

                        ChangeImagesTarget.TEMPLATE -> {
                            val templateId = recordsRepository.get(recordId)!!.template.dbId
                            editTemplateUseCase.execute(
                                templateId = templateId,
                                images = images,
                                title = title,
                                description = description,
                            )
                        }
                    }
                    GetMacrosWorker.setWorkRequest(
                        appContext = App.appContext,
                        recordId = recordId,
                        force = true,
                    )
                }
                popDialog()
                popDialog()
                DiaryWidgetScreen.reload()
            }
    }

    private fun runSafely(task: suspend () -> Unit): Job {
        return viewModelScope.launch(errorHandler) {
            task()
        }
    }

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        val message = when {
            exception is DomainError -> exception.message()
            else -> exception.message ?: exception.cause?.message
        }
        viewModelScope.launch {
            _uiUpdates.send(
                ModalUIUpdates.Error(
                    message?.ellipsize(
                        300
                    )
                        ?: "Oops. Something went wrong. The issue has been logged and our engineers are looking into it."
                )
            )
        }
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
