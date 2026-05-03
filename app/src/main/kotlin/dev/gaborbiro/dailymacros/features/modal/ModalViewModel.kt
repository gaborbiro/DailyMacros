package dev.gaborbiro.dailymacros.features.modal

import android.net.Uri
import androidx.annotation.UiThread
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.App
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.common.RepeatRecordUseCase
import dev.gaborbiro.dailymacros.features.common.workers.GetMacrosWorker
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.ImageInputType
import dev.gaborbiro.dailymacros.features.modal.model.ModalUiState
import dev.gaborbiro.dailymacros.features.modal.model.ModalUiUpdates
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.DeleteRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodRecognitionUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.UpdateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.features.widget.DiaryWidgetScreen
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.DomainError
import dev.gaborbiro.dailymacros.features.modal.usecase.GetVariabilityMatchForTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.NoVariabilityProfileLoadedException
import dev.gaborbiro.dailymacros.features.modal.usecase.TemplateVariabilityMatch
import dev.gaborbiro.dailymacros.repositories.records.TemplateVariabilityPreviewMapper
import dev.gaborbiro.dailymacros.repositories.records.VariabilityProfileMapper
import dev.gaborbiro.dailymacros.repositories.records.domain.VariabilityRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityPreviewContent
import ellipsize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

internal class ModalViewModel(
    private val imageStore: ImageStore,
    private val recordsRepository: RecordsRepository,
    private val variabilityRepository: VariabilityRepository,
    private val variabilityProfileMapper: VariabilityProfileMapper,
    private val templateVariabilityPreviewMapper: TemplateVariabilityPreviewMapper,
    private val getVariabilityMatchForTemplateUseCase: GetVariabilityMatchForTemplateUseCase,
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
    private val analyticsLogger: AnalyticsLogger,
    private val uiMapper: ModalUiMapper,
) : ViewModel() {

    companion object {
        enum class ChangeImagesTarget {
            RECORD, TEMPLATE
        }
    }

    private val _uiState = MutableStateFlow(ModalUiState())
    val uiState: StateFlow<ModalUiState> = _uiState.asStateFlow()

    private val _uiUpdates = Channel<ModalUiUpdates>(Channel.BUFFERED)
    val uiUpdates: Flow<ModalUiUpdates> = _uiUpdates.receiveAsFlow()

    private var recogniseFoodJob: Job? = null

    private var recordDetailsJob: Job? = null

    /** Restores record details after closing the variant picker without applying. */
    private var pendingRecordDetailsRestore: Pair<Long, Boolean>? = null

    @UiThread
    fun onCreateRecordWithCameraDeeplink() {
        setRoot(DialogHandle.ImageInput(type = ImageInputType.Camera))
    }

    @UiThread
    fun onCreateRecordWithBrowseImagesDeeplink() {
        setRoot(DialogHandle.ImageInput(type = ImageInputType.BrowseImages))
    }

    @UiThread
    fun onCreateRecordWithTextDeeplink() {
        setRoot(
            DialogHandle.RecordDetailsDialog.Edit(
                title = TextFieldValue(),
                titleHint = "Give your meal a title (or let AI figure it out from photo)",
                description = TextFieldValue(),
                images = emptyList(),
                recognisedFood = null,
                showProgressIndicator = false,
            )
        )
    }

    fun viewRecordImageDeeplink(recordId: Long) {
        runSafely {
            getRecordImageUseCase.execute(recordId)
                ?.let { imageDialog ->
                    setRoot(imageDialog)
                }
                ?: run {
                    _uiUpdates.send(ModalUiUpdates.Close)
                }
        }
    }

    fun onViewTemplateImageDeeplink(templateId: Long) {
        runSafely {
            getTemplateImageUseCase.execute(templateId)
                ?.let { imageDialog ->
                    setRoot(imageDialog)
                }
                ?: run {
                    _uiUpdates.send(ModalUiUpdates.Close)
                }
        }
    }

    @UiThread
    fun onViewRecordDetailsDeeplink(recordId: Long) {
        openRecordDetails(recordId, edit = true)
    }

    private fun openRecordDetails(recordId: Long, edit: Boolean) {
        recordDetailsJob?.cancel()
        recordDetailsJob = runSafely {
            recordsRepository.observe(recordId)
                .collect { record ->
                    val match = runCatching {
                        getVariabilityMatchForTemplateUseCase.execute(record.template.dbId)
                    }.getOrElse { t ->
                        if (t is NoVariabilityProfileLoadedException) {
                            TemplateVariabilityMatch(
                                preview = TemplateVariabilityPreviewContent(
                                    bannerText = "",
                                    slots = emptyList(),
                                    archetypePickerLabel = "",
                                ),
                                profileJson = null,
                                minedAtEpochMs = 0L,
                            )
                        } else {
                            throw t
                        }
                    }
                    val previewForDialog =
                        match.preview.slots.takeIf { it.isNotEmpty() }?.let { match.preview }

                    val dialog = DialogHandle.RecordDetailsDialog.View(
                        recordId = recordId,
                        templateDbId = record.template.dbId,
                        title = TextFieldValue(record.template.name),
                        description = TextFieldValue(record.template.description),
                        images = record.template.images,
                        nutrientBreakdown = uiMapper.mapNutrientBreakdowns(record),
                        allowEdit = edit,
                        titleHint = "Give your meal a title",
                        titleValidationError = null,
                        templateVariabilityPreview = previewForDialog,
                        variabilityProfileJson = match.profileJson,
                        variabilityProfileMinedAtEpochMs = match.minedAtEpochMs,
                    )
                    setRoot(dialog)
                }
        }
    }

    @UiThread
    fun onSelectRecordActionDeeplink(recordId: Long) {
        runSafely {
            val title = recordsRepository.get(recordId)?.template?.name ?: ""
            setRoot(DialogHandle.SelectRecordActionDialog(recordId, title))
        }
    }

    @UiThread
    fun onSelectTemplateActionDeeplink(templateId: Long) {
        runSafely {
            val title =
                recordsRepository.getRecordsByTemplate(templateId).firstOrNull()?.template?.name
                    ?: ""
            setRoot(DialogHandle.SelectTemplateActionDialog(templateId, title))
        }
    }

    @UiThread
    fun onImagesSelected(uris: List<Uri>) {
        runSafely {
            val persistedFilenames = uris.map {
                saveImageUseCase.execute(it)
            }

            when (val root = _uiState.value.rootDialog) {
                is DialogHandle.RecordDetailsDialog.Edit -> {
                    val updatedImages = root.images + persistedFilenames
                    setRoot(
                        root.copy(
                            images = updatedImages,
                            recognisedFood = null,
                        )
                    )
                    if (root.images.isEmpty()) { // only run the food recognition automatically for the first (batch of) images
                        runFoodRecognition(updatedImages)
                    }
                }

                is DialogHandle.RecordDetailsDialog.View -> {
                    setRoot(root.copy(images = root.images + persistedFilenames))
                }

                else -> {
                    setRoot(
                        DialogHandle.RecordDetailsDialog.Edit(
                            title = TextFieldValue(),
                            titleHint = "Give your meal a title (or wait a bit for the AI to figure it out)",
                            description = TextFieldValue(),
                            images = persistedFilenames,
                            recognisedFood = null,
                        )
                    )
                    runFoodRecognition(persistedFilenames)
                }
            }
        }
    }

    @UiThread
    fun onImagesShared(uris: List<Uri>) {
        runSafely {
            val persistedFilenames = uris.map {
                saveImageUseCase.execute(it)
            }

            setRoot(
                DialogHandle.RecordDetailsDialog.Edit(
                    title = TextFieldValue(),
                    titleHint = "Give yur meal a title (or wait a bit for the AI to figure it out)",
                    description = TextFieldValue(),
                    images = persistedFilenames,
                    recognisedFood = null,
                )
            )
            runFoodRecognition(persistedFilenames)
        }
    }

    private fun runFoodRecognition(images: List<String>) {
        recogniseFoodJob?.cancel()
        recogniseFoodJob = runSafely {
            updateRoot<DialogHandle.RecordDetailsDialog.Edit> {
                it.copy(showProgressIndicator = true)
            }
            try {
                val recognisedFood = foodRecognitionUseCase.execute(images)
                updateRoot<DialogHandle.RecordDetailsDialog.Edit> { currentUiState ->
                    val title = currentUiState.title.takeIf { it.text.isNotBlank() }
                        ?: recognisedFood.title
                            ?.takeIf { it.isNotBlank() }
                            ?.let {
                                TextFieldValue(it, selection = TextRange(it.length))
                            }
                        ?: TextFieldValue()
                    currentUiState.copy(
                        recognisedFood = recognisedFood,
                        title = title,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                currentCoroutineContext().ensureActive()
                throw t
            } finally {
                if (!currentCoroutineContext().job.isCancelled) {
                    updateRoot<DialogHandle.RecordDetailsDialog.Edit> {
                        it.copy(
                            showProgressIndicator = false,
                            showRunAIButton = true,
                        )
                    }
                }
            }
        }
    }

    fun onNoImageSelected() {
        if (_uiState.value.overlayDialog is DialogHandle.ImageInput) {
            popOverlay()
        } else {
            closeAll()
        }
    }

    @UiThread
    fun onRepeatRecordButtonTapped(recordId: Long) {
        closeAll()
        runSafely {
            repeatRecordUseCase.execute(recordId)
            DiaryWidgetScreen.reload()
        }
    }

    @UiThread
    fun onRepeatTemplateButtonTapped(templateId: Long) {
        runSafely {
            createRecordFromTemplateUseCase.execute(templateId)
            DiaryWidgetScreen.reload()
            closeAll()
        }
    }

    @UiThread
    fun onRecordDetailsButtonTapped(recordId: Long) {
        openRecordDetails(recordId, edit = true)
    }

    @UiThread
    fun onTemplateDetailsButtonTapped(templateId: Long) {
        runSafely {
            recordsRepository.getRecordsByTemplate(templateId).firstOrNull()
                ?.let {
                    openRecordDetails(it.recordId, edit = false)
                }
        }
    }

    @UiThread
    fun onRemoveFromQuickPicksTapped(templateId: Long) {
        closeAll()
        runSafely {
            recordsRepository.addQuickPickOverride(templateId, Template.QuickPickOverride.EXCLUDE)
            DiaryWidgetScreen.reload()
        }
    }

    @UiThread
    fun onAddToQuickPicksTapped(recordId: Long) {
        closeAll()
        runSafely {
            val templateId = recordsRepository.get(recordId)?.template?.dbId ?: return@runSafely
            recordsRepository.addQuickPickOverride(templateId, Template.QuickPickOverride.INCLUDE)
            DiaryWidgetScreen.reload()
        }
    }

    @UiThread
    fun onDeleteTapped(recordId: Long) {
        closeAll()
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
    fun onDialogDismissRequested(dialog: DialogHandle) {
        if (dialog == _uiState.value.overlayDialog) {
            popOverlay()
        } else if (dialog is DialogHandle.TemplateVariantPickerDialog) {
            onVariantPickerCancelTapped()
            return
        } else {
            closeAll()
        }
        (dialog as? DialogHandle.RecordDetailsDialog.Edit)?.let {
            runSafely {
                it.images.forEach { img ->
                    imageStore.delete(img)
                }
            }
        }
    }

    @UiThread
    fun onTitleChanged(title: TextFieldValue) {
        updateRoot<DialogHandle.RecordDetailsDialog> {
            it
                .withTitle(title)
                .withTitleValidationError(null)
        }
    }

    @UiThread
    fun onDescriptionChanged(description: TextFieldValue) {
        updateRoot<DialogHandle.RecordDetailsDialog> {
            it.withDescription(description)
        }
    }

    @UiThread
    fun onImageTapped(image: String) {
        val root = _uiState.value.rootDialog
        val allImages = (root as? DialogHandle.RecordDetailsDialog)?.images ?: listOf(image)
        val index = allImages.indexOf(image).coerceAtLeast(0)
        pushOverlay(DialogHandle.ViewImageDialog("", allImages, index))
    }

    @UiThread
    fun onImageDeleteTapped(image: String) {
        updateRoot<DialogHandle.RecordDetailsDialog> {
            when (it) {
                is DialogHandle.RecordDetailsDialog.View -> it.copy(images = it.images - image)
                is DialogHandle.RecordDetailsDialog.Edit -> it.copy(images = it.images - image)
            }
        }
    }

    @UiThread
    fun onAddImageViaCameraTapped() {
        pushOverlay(DialogHandle.ImageInput(type = ImageInputType.Camera))
    }

    @UiThread
    fun onAddImageViaPickerTapped() {
        pushOverlay(DialogHandle.ImageInput(type = ImageInputType.BrowseImages))
    }

    @UiThread
    fun onSubmitButtonTapped() {
        (_uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog)
            ?.let {
                runSafely {
                    when (it) {
                        is DialogHandle.RecordDetailsDialog.Edit -> {
                            handleCreateRecordDetailsSubmitted(it)
                        }

                        is DialogHandle.RecordDetailsDialog.View -> {
                            handleEditRecordDialogSubmitted(it)
                        }
                    }
                }
            }
    }

    fun onImagesInfoButtonTapped() {
        pushOverlay(
            DialogHandle.InfoDialog(
                "You can add multiple photos.\nPhotos of nutritional labels are especially helpful." +
                        "\n\nYou can edit the entry later. You don’t need to collect all details up front." +
                        "\n\nLogging the meal is more important than the exact time. Just don’t leave it for the next day - dates can’t be changed retroactively."
            )
        )
    }

    fun onRunAIButtonTapped() {
        updateRoot<DialogHandle.RecordDetailsDialog.Edit> {
            runFoodRecognition(it.images)
            it.copy(
                title = TextFieldValue()
            )
        }
    }

    @UiThread
    fun onVariabilityDifferentMealLinkTapped(
        recordId: Long,
        templateId: Long,
        profileJson: String,
        profileMinedAtEpochMs: Long,
        preview: TemplateVariabilityPreviewContent,
    ) {
        runSafely {
            val slots = preview.slots
            if (slots.isEmpty()) return@runSafely
            val archetypeKey = slots.first().archetypeKey
            val archetypeLabel = preview.archetypePickerLabel.ifBlank { slots.first().archetypeDisplayName }
            val profile = variabilityProfileMapper.parseProfileJson(profileJson, profileMinedAtEpochMs)
            val archetype = profile.archetypes.find { it.archetypeKey == archetypeKey }
            val initialSelections = slots.associate { sp ->
                val slot = archetype?.slots?.find { it.slotKey == sp.slotKey }
                val variant = slot?.variants?.find { v ->
                    v.evidence.any { it.templateId == templateId }
                }
                sp.slotKey to (variant?.variantKey ?: sp.variants.first().variantKey)
            }
            val existing = templateVariabilityPreviewMapper.existingCombinationKeysForArchetype(
                profile.archetypes,
                archetypeKey,
                slots,
            )
            val currentKey = templateVariabilityPreviewMapper.combinationKeyForTemplateInArchetype(
                profile.archetypes,
                archetypeKey,
                slots,
                templateId,
            )
            val edit =
                (_uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.View)?.allowEdit == true
            pendingRecordDetailsRestore = recordId to edit
            recordDetailsJob?.cancel()
            recordDetailsJob = null
            setRoot(
                DialogHandle.TemplateVariantPickerDialog(
                    recordId = recordId,
                    templateId = templateId,
                    profileJson = profileJson,
                    profileMinedAtEpochMs = profileMinedAtEpochMs,
                    archetypeKey = archetypeKey,
                    archetypeDisplayName = archetypeLabel,
                    slots = slots,
                    initialSlotSelections = initialSelections,
                    existingCombinationKeys = existing,
                    currentTemplateCombinationKey = currentKey,
                ),
            )
        }
    }

    @UiThread
    fun onVariantPickerCancelTapped() {
        val restore = pendingRecordDetailsRestore
        pendingRecordDetailsRestore = null
        if (restore != null) {
            openRecordDetails(restore.first, restore.second)
        } else {
            closeAll()
        }
    }

    @UiThread
    fun onVariantPickerConfirmed(selection: Map<String, String>) {
        val dlg = _uiState.value.rootDialog as? DialogHandle.TemplateVariantPickerDialog ?: return
        runSafely {
            val profile = variabilityProfileMapper.parseProfileJson(dlg.profileJson, dlg.profileMinedAtEpochMs)
            val comboKey = templateVariabilityPreviewMapper.combinationKey(dlg.slots, selection)
            val reuseId = templateVariabilityPreviewMapper.templateIdForCombinationInArchetype(
                profile.archetypes,
                dlg.archetypeKey,
                dlg.slots,
                comboKey,
            ) ?: return@runSafely
            val record = recordsRepository.get(dlg.recordId) ?: return@runSafely
            if (reuseId == record.template.dbId) {
                pendingRecordDetailsRestore = null
                openRecordDetails(dlg.recordId, true)
                return@runSafely
            }
            val newTemplate = recordsRepository.getTemplate(reuseId)
            recordsRepository.updateRecord(record.copy(template = newTemplate))
            DiaryWidgetScreen.reload()
            // Do not enqueue nutrient analysis: "Use this" selects an existing mined template that
            // already has its own images/macros; we only change which template this record points to.
            pendingRecordDetailsRestore = null
            openRecordDetails(dlg.recordId, true)
        }
    }

    private suspend fun handleCreateRecordDetailsSubmitted(
        dialogHandle: DialogHandle.RecordDetailsDialog.Edit,
    ) {
        val title = dialogHandle.title.text.trim()
        val description = dialogHandle.description.text.trim()
        val images = dialogHandle.images

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
                closeAll()
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
        dialogHandle: DialogHandle.RecordDetailsDialog.View,
    ) {
        val title = dialogHandle.title.text.trim()
        val description = dialogHandle.description.text.trim()

        val result = validateEditRecordUseCase.execute(
            recordId = dialogHandle.recordId,
            title = title,
            description = description,
        )
        when (result) {
            is EditValidationResult.ConfirmMultipleEdit -> {
                pushOverlay(
                    DialogHandle.EditTargetConfirmationDialog(
                        recordId = dialogHandle.recordId,
                        images = dialogHandle.images,
                        count = result.count,
                        title = title,
                        description = description,
                    )
                )
            }

            is EditValidationResult.Valid -> {
                closeAll()
                updateRecordWithNewTemplateUseCase.execute(
                    recordId = dialogHandle.recordId,
                    images = dialogHandle.images,
                    title = title,
                    description = description,
                )
                DiaryWidgetScreen.reload()
                GetMacrosWorker.setWorkRequest(
                    appContext = App.appContext,
                    recordId = dialogHandle.recordId,
                    force = true,
                )
            }

            is EditValidationResult.Error -> {
                applyValidationError(result.message)
            }
        }
    }

    private fun applyValidationError(message: String?) {
        updateRoot<DialogHandle.RecordDetailsDialog> {
            it.withTitleValidationError(titleValidationError = message)
        }
    }

    @UiThread
    fun onEditTargetConfirmed(target: ChangeImagesTarget) {
        (_uiState.value.overlayDialog as? DialogHandle.EditTargetConfirmationDialog)
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
                closeAll()
                DiaryWidgetScreen.reload()
            }
    }

    private fun runSafely(task: suspend () -> Unit): Job {
        return viewModelScope.launch(errorHandler) {
            task()
        }
    }

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        if (exception is CancellationException) return@CoroutineExceptionHandler
        analyticsLogger.logError(exception)
        val message = when {
            exception is DomainError -> exception.message()
            else -> exception.message ?: exception.cause?.message
        }
        viewModelScope.launch {
            _uiUpdates.send(
                ModalUiUpdates.Error(
                    message?.ellipsize(
                        300
                    )
                        ?: "Oops. Something went wrong. The issue has been logged and our engineers are looking into it."
                )
            )
        }
    }

    private fun setRoot(dialog: DialogHandle) {
        _uiState.update {
            it.copy(rootDialog = dialog, overlayDialog = null)
        }
    }

    private fun pushOverlay(dialog: DialogHandle) {
        _uiState.update {
            it.copy(overlayDialog = dialog)
        }
    }

    private fun popOverlay() {
        _uiState.update {
            it.copy(overlayDialog = null)
        }
    }

    private fun closeAll() {
        recogniseFoodJob?.cancel()
        recordDetailsJob?.cancel()
        _uiState.update {
            it.copy(
                rootDialog = null,
                overlayDialog = null,
            )
        }
        viewModelScope.launch {
            _uiUpdates.send(ModalUiUpdates.Close)
        }
    }

    private inline fun <reified T : DialogHandle> updateRoot(transform: (T) -> DialogHandle) {
        _uiState.update {
            val root = it.rootDialog
            if (root is T) {
                it.copy(rootDialog = transform(root))
            } else {
                it
            }
        }
    }
}
