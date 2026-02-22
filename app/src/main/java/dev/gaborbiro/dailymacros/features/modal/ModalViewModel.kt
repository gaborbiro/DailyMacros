package dev.gaborbiro.dailymacros.features.modal

import android.net.Uri
import androidx.annotation.UiThread
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.AnalyticsLogger
import dev.gaborbiro.dailymacros.App
import dev.gaborbiro.dailymacros.data.db.model.entity.QuickPickOverrideEntity
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.common.DeleteRecordUseCase
import dev.gaborbiro.dailymacros.features.common.NutrientsUIMapper
import dev.gaborbiro.dailymacros.features.common.RepeatRecordUseCase
import dev.gaborbiro.dailymacros.features.common.message
import dev.gaborbiro.dailymacros.features.common.workers.GetMacrosWorker
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.features.modal.model.ImageInputType
import dev.gaborbiro.dailymacros.features.modal.model.NutrientsBreakdownUiModel
import dev.gaborbiro.dailymacros.features.modal.model.ModalUIUpdates
import dev.gaborbiro.dailymacros.features.modal.model.ModalViewState
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.EditTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodRecognitionUseCase
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
    private val foodRecognitionUseCase: FoodRecognitionUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase,
    private val nutrientsUIMapper: NutrientsUIMapper,
    private val analyticsLogger: AnalyticsLogger,
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

    private var recogniseFoodJob: Job? = null

    @UiThread
    fun onCreateRecordWithCameraDeeplink() {
        pushDialog(
            DialogState.ImageInput(type = ImageInputType.Camera)
        )
    }

    @UiThread
    fun onCreateRecordWithBrowseImagesDeeplink() {
        pushDialog(
            DialogState.ImageInput(type = ImageInputType.BrowseImages)
        )
    }

    @UiThread
    fun onCreateRecordWithTextDeeplink() {
        pushDialog(
            DialogState.RecordDetailsDialog.Edit(
                title = TextFieldValue(),
                titleHint = "Describe your meal (or snap a photo)",
                description = TextFieldValue(),
                images = emptyList(),
                recognisedFood = null,
                showProgressIndicator = false,
            )
        )
    }

    fun viewRecordImageDeeplink(recordId: Long) {
        runSafely {
            getRecordImageUseCase.execute(recordId, thumbnail = false)
                ?.let { imageDialog ->
                    pushDialog(imageDialog)
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
                    pushDialog(imageDialog)
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
                    val macros = record.template.nutrientsBreakdown
                        ?.let {
                            NutrientsBreakdownUiModel(
                                calories = nutrientsUIMapper.formatCalories(value = record.template.nutrientsBreakdown.calories, isShort = false, withLabel = true),
                                protein = nutrientsUIMapper.formatProtein(value = record.template.nutrientsBreakdown.protein, isShort = false, withLabel = true),
                                fat = nutrientsUIMapper.formatFat(value = record.template.nutrientsBreakdown.fat, saturated = null, isShort = false, withLabel = true),
                                ofWhichSaturated = nutrientsUIMapper.formatSaturatedFat(value = record.template.nutrientsBreakdown.ofWhichSaturated, isShort = false, withLabel = true),
                                carbs = nutrientsUIMapper.formatCarbs(value = record.template.nutrientsBreakdown.carbs, sugar = null, addedSugar = null, isShort = false, withLabel = true),
                                ofWhichSugar = nutrientsUIMapper.formatSugar(value = record.template.nutrientsBreakdown.ofWhichSugar, isShort = false, withLabel = true),
                                ofWhichAddedSugar = nutrientsUIMapper.formatAddedSugar(value = record.template.nutrientsBreakdown.ofWhichAddedSugar, isShort = false, withLabel = true),
                                salt = nutrientsUIMapper.formatSalt(value = record.template.nutrientsBreakdown.salt, isShort = false, withLabel = true),
                                fibre = nutrientsUIMapper.formatFibre(value = record.template.nutrientsBreakdown.fibre, isShort = false, withLabel = true),
                                notes = record.template.nutrientsBreakdown.notes,
                            )
                        }
                    val dialog = DialogState.RecordDetailsDialog.View(
                        recordId = recordId,
                        title = TextFieldValue(record.template.name),
                        description = TextFieldValue(record.template.description),
                        images = record.template.images,
                        nutrientsBreakdown = macros,
                        allowEdit = edit,
                        titleHint = "Describe your meal",
                        titleValidationError = null,
                    )
                    setDialogs(listOf(dialog))
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
            val title =
                recordsRepository.getRecordsByTemplate(templateId).firstOrNull()?.template?.name
                    ?: ""
            pushDialog(
                DialogState.SelectTemplateActionDialog(templateId, title)
            )
        }
    }

    @UiThread
    fun onImagesSelected(uris: List<Uri>, imageInputDialog: DialogState.ImageInput) {
        recogniseFoodJob?.cancel()
        recogniseFoodJob = runSafely {
            var dialogs = _viewState.value.dialogs
            val persistedFilenames = uris.map {
                saveImageUseCase.execute(it)
            }

            dialogs = dialogs - imageInputDialog

            dialogs = dialogs.replaceInstances<DialogState.RecordDetailsDialog.Edit> {
                it.copy(
                    images = it.images + persistedFilenames,
                    recognisedFood = null, // will be replaced by AI
                    showProgressIndicator = true,
                )
            }

            dialogs = dialogs.replaceInstances<DialogState.RecordDetailsDialog.View> {
                it.copy(
                    images = it.images + persistedFilenames,
                )
            }

            if (dialogs.isEmpty()) {
                dialogs = listOf(
                    DialogState.RecordDetailsDialog.Edit(
                        title = TextFieldValue(),
                        titleHint = "Describe your meal (or tap one of the AI suggestions)",
                        description = TextFieldValue(),
                        images = persistedFilenames,
                        recognisedFood = null,
                        showProgressIndicator = true,
                    )
                )
            }

            dialogs.filterIsInstance<DialogState.RecordDetailsDialog.Edit>().firstOrNull()
                ?.let {
                    runFoodRecognition(it.images)
                }

            setDialogs(dialogs)
        }
    }

    @UiThread
    fun onImagesShared(uris: List<Uri>) {
        recogniseFoodJob?.cancel()
        recogniseFoodJob = runSafely {
            val persistedFilenames = uris.map {
                saveImageUseCase.execute(it)
            }

            pushDialog(
                DialogState.RecordDetailsDialog.Edit(
                    title = TextFieldValue(),
                    titleHint = "Describe your meal (or tap one of the AI suggestions)",
                    description = TextFieldValue(),
                    images = persistedFilenames,
                    recognisedFood = null,
                    showProgressIndicator = true,
                )
            )
            runFoodRecognition(persistedFilenames)
        }
    }

    private fun runFoodRecognition(images: List<String>) {
        runSafely {
            try {
                val recognisedFood = foodRecognitionUseCase.execute(images)
                updateDialogsOfType<DialogState.RecordDetailsDialog.Edit> {
                    val title = if (it.title.text.isBlank()) {
                        val title = recognisedFood.title ?: ""
                        TextFieldValue(title, selection = TextRange(title.length))
                    } else {
                        it.title
                    }
                    it.copy(
                        recognisedFood = recognisedFood,
                        title = title,
                        showProgressIndicator = false,
                    )
                }
            } catch (t: Throwable) {
                throw t
            } finally {
                updateDialogsOfType<DialogState.RecordDetailsDialog.Edit> {
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
    fun onRemoveFromQuickPicksTapped(templateId: Long) {
        popDialog()
        runSafely {
            recordsRepository.addQuickPickOverride(templateId, QuickPickOverrideEntity.OverrideType.EXCLUDE)
            DiaryWidgetScreen.reload()
        }
    }

    @UiThread
    fun onAddToQuickPicksTapped(recordId: Long) {
        popDialog()
        runSafely {
            val templateId = recordsRepository.get(recordId)?.template?.dbId ?: return@runSafely
            recordsRepository.addQuickPickOverride(templateId, QuickPickOverrideEntity.OverrideType.INCLUDE)
            DiaryWidgetScreen.reload()
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
        recogniseFoodJob?.cancel()
        (dialog as? DialogState.RecordDetailsDialog.Edit)?.let {
            runSafely {
                dialog.images.forEach {
                    imageStore.delete(it)
                }
            }
        }
    }

    @UiThread
    fun onTitleChanged(title: TextFieldValue) {
        updateDialogsOfType<DialogState.RecordDetailsDialog> {
            it
                .withTitle(title)
                .withTitleValidationError(null)
        }
    }

    @UiThread
    fun onDescriptionChanged(description: TextFieldValue) {
        updateDialogsOfType<DialogState.RecordDetailsDialog> {
            it.withDescription(description)
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
    fun onImageDeleteTapped(image: String) {
        var dialogs = _viewState.value.dialogs
        dialogs = dialogs.replaceInstances<DialogState.RecordDetailsDialog.View> {
            it.copy(
                images = it.images - image,
            )
        }
        dialogs = dialogs.replaceInstances<DialogState.RecordDetailsDialog.Edit> {
            it.copy(
                images = it.images - image,
            )
        }
        setDialogs(dialogs)
    }

    @UiThread
    fun onAddImageViaCameraTapped(dialogState: DialogState.RecordDetailsDialog) {
        pushDialog(
            DialogState.ImageInput(type = ImageInputType.Camera)
        )
    }

    @UiThread
    fun onAddImageViaPickerTapped(dialogState: DialogState.RecordDetailsDialog) {
        pushDialog(
            DialogState.ImageInput(type = ImageInputType.BrowseImages)
        )
    }

    @UiThread
    fun onSubmitButtonTapped() {
        _viewState.value.dialogs
            .filterIsInstance<DialogState.RecordDetailsDialog>()
            .firstOrNull()
            ?.let {
                runSafely {
                    when (it) {
                        is DialogState.RecordDetailsDialog.Edit -> {
                            handleCreateRecordDetailsSubmitted(it)
                        }

                        is DialogState.RecordDetailsDialog.View -> {
                            handleEditRecordDialogSubmitted(it)
                        }
                    }
                }
            }
    }

    fun onImagesInfoButtonTapped() {
        pushDialog(DialogState.InfoDialog("You can add as many images as you like. Nutritional labels are particularly useful to the AI. You can also add more photos or update things later, don't worry about gathering all info right away."))
    }

    private suspend fun handleCreateRecordDetailsSubmitted(
        dialogState: DialogState.RecordDetailsDialog,
    ) {
        val title = dialogState.title.text.trim()
        val description = dialogState.description.text.trim()
        val images = dialogState.images

        val result = validateCreateRecordUseCase.execute(
            images = images,
            title = title,
            description = description
        )

        when (result) {
            is CreateValidationResult.Error -> {
                applyValidationError(result.message)
            }

            is CreateValidationResult.Valid -> {
                popDialog()
                val recordId = createRecordWithNewTemplateUseCase.execute(
                    images = images,
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
        dialogState: DialogState.RecordDetailsDialog.View,
    ) {
        val title = dialogState.title.text.trim()
        val description = dialogState.description.text.trim()

        val result = validateEditRecordUseCase.execute(
            recordId = dialogState.recordId,
            title = title,
            description = description,
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
        updateDialogsOfType<DialogState.RecordDetailsDialog> {
            it.withTitleValidationError(titleValidationError = message)
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
        analyticsLogger.logError(exception)
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

    private fun setDialogs(dialogs: List<DialogState>) {
        _viewState.update {
            it.copy(
                dialogs = dialogs,
                close = dialogs.isEmpty(),
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
