package dev.gaborbiro.dailymacros.features.modal

import android.app.Application
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.modal.model.ChangeImagesTarget
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.ImageInputType
import dev.gaborbiro.dailymacros.features.modal.model.ModalUiState
import dev.gaborbiro.dailymacros.features.modal.model.ModalUiUpdates
import dev.gaborbiro.dailymacros.features.modal.model.recordDetailsEditPristineSnapshot
import dev.gaborbiro.dailymacros.features.modal.model.toPickerOptions
import dev.gaborbiro.dailymacros.features.modal.usecase.ApplyConfirmedSharedTemplateEditUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ApplyQuickPickOverrideAndReloadWidgetUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.BuildRecordDetailsViewDialogUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.EditValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.ExportImageToGalleryUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodRecognitionUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ResolveFirstRecordIdForTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.UpdateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.features.shared.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.shared.ErrorUiMapper
import dev.gaborbiro.dailymacros.features.shared.ListMealVariantsForTemplateUseCase
import dev.gaborbiro.dailymacros.features.shared.NutrientAnalysisWorker
import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import ellipsize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class ModalViewModel @Inject constructor(
    application: Application,
    private val modalUiMapper: ModalUiMapper,
    private val imageStore: ImageStore,
    private val recordsRepository: RecordsRepository,
    private val buildRecordDetailsViewDialogUseCase: BuildRecordDetailsViewDialogUseCase,
    private val resolveFirstRecordIdForTemplateUseCase: ResolveFirstRecordIdForTemplateUseCase,
    private val listMealVariantsForTemplateUseCase: ListMealVariantsForTemplateUseCase,
    private val settingsRepository: SettingsRepository,
    private val createRecordFromTemplateUseCase: CreateRecordFromTemplateUseCase,
    private val createTemplateUseCase: CreateTemplateUseCase,
    private val createRecordWithNewTemplateUseCase: CreateRecordWithNewTemplateUseCase,
    private val updateRecordWithNewTemplateUseCase: UpdateRecordWithNewTemplateUseCase,
    private val validateEditRecordUseCase: ValidateEditRecordUseCase,
    private val validateCreateRecordUseCase: ValidateCreateRecordUseCase,
    private val saveImageUseCase: SaveImageUseCase,
    private val exportImageToGalleryUseCase: ExportImageToGalleryUseCase,
    private val getRecordImageUseCase: GetRecordImageUseCase,
    private val getTemplateImageUseCase: GetTemplateImageUseCase,
    private val foodRecognitionUseCase: FoodRecognitionUseCase,
    private val applyQuickPickOverrideAndReloadWidgetUseCase: ApplyQuickPickOverrideAndReloadWidgetUseCase,
    private val applyConfirmedSharedTemplateEditUseCase: ApplyConfirmedSharedTemplateEditUseCase,
    private val analyticsLogger: AnalyticsLogger,
    private val errorUiMapper: ErrorUiMapper,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ModalUiState())
    val uiState: StateFlow<ModalUiState> = _uiState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<ModalUiUpdates>(extraBufferCapacity = 16)
    val uiUpdates: SharedFlow<ModalUiUpdates> = _uiUpdates.asSharedFlow()

    private var recogniseFoodJob: Job? = null
    private var photoExportJob: Job? = null

    private var recordDetailsJob: Job? = null

    private companion object {
        /** Matches widget reload ranking window (see ReloadWorker). */
        const val QUICK_PICK_RANK_CHECK_COUNT = 30
    }

    fun onCreateRecordWithCameraDeeplinkReceived() {
        setRoot(emptyRecordDetailsEdit())
        pushOverlay(DialogHandle.ImageInput(type = ImageInputType.Camera))
    }

    fun onCreateRecordWithBrowseImagesDeeplinkReceived() {
        setRoot(emptyRecordDetailsEdit())
        pushOverlay(DialogHandle.ImageInput(type = ImageInputType.BrowseImages))
    }

    fun onCreateRecordWithTextDeeplinkReceived() {
        setRoot(emptyRecordDetailsEdit())
    }

    fun onPhotoRecognitionDetailsDeeplinkReceived(
        recognisedTitle: String,
        imageFilename: String,
        sourceMediaStoreId: Long? = null,
    ) {
        sourceMediaStoreId?.let { pendingImageSources[imageFilename] = it }
        val titleValue = TextFieldValue(recognisedTitle, TextRange(recognisedTitle.length))
        setRoot(
            DialogHandle.RecordDetailsDialog.Edit(
                title = titleValue,
                titleHint = "Title",
                description = TextFieldValue(),
                imageFilenames = listOf(imageFilename),
                recognisedFood = null,
                showProgressIndicator = false,
                pristineSnapshot = recordDetailsEditPristineSnapshot(
                    title = titleValue,
                    description = TextFieldValue(),
                    imageFilenames = listOf(imageFilename),
                ),
            )
        )
    }

    fun onViewRecordImageDeeplinkReceived(recordId: Long) {
        runSafely("Couldn't open the image") {
            getRecordImageUseCase.execute(recordId)
                ?.let { imageDialog ->
                    setRoot(imageDialog)
                }
                ?: run {
                    _uiUpdates.emit(ModalUiUpdates.Close)
                }
        }
    }

    fun onViewTemplateImageDeeplinkReceived(templateId: Long) {
        runSafely("Couldn't open the image") {
            getTemplateImageUseCase.execute(templateId)
                ?.let { imageDialog ->
                    setRoot(imageDialog)
                }
                ?: run {
                    _uiUpdates.emit(ModalUiUpdates.Close)
                }
        }
    }

    fun onViewRecordDetailsDeeplinkReceived(recordId: Long) {
        openRecordDetails(recordId, edit = true)
    }

    fun onViewTemplateDetailsDeeplinkReceived(templateId: Long) {
        onTemplateDetailsButtonTapped(templateId)
    }

    fun onQuickPickWidgetConfirmDeeplinkReceived(templateId: Long, templateName: String) {
        if (settingsRepository.getQuickPickConfirmationEnabled().not()) {
            logMealFromTemplate(templateId)
            return
        }
        setRoot(
            DialogHandle.QuickPickWidgetConfirmDialog(
                templateId = templateId,
                templateName = templateName,
            )
        )
    }

    fun onQuickPickWidgetLogAgainTapped() {
        val dialog = _uiState.value.rootDialog as? DialogHandle.QuickPickWidgetConfirmDialog ?: return
        logMealFromTemplate(dialog.templateId)
    }

    fun onQuickPickWidgetDontShowAgainChanged(dontShowAgain: Boolean) {
        val dialog = _uiState.value.rootDialog as? DialogHandle.QuickPickWidgetConfirmDialog ?: return
        settingsRepository.setQuickPickConfirmationEnabled(dontShowAgain.not())
        _uiState.update { it.copy(rootDialog = dialog.copy(dontShowAgain = dontShowAgain)) }
    }

    private fun logMealFromTemplate(templateId: Long) {
        viewModelScope.launch {
            try {
                val recordId = createRecordFromTemplateUseCase.execute(templateId)
                scheduleMacroAnalysisForRecordIfTemplateIncomplete(recordId, templateId)
                _uiUpdates.emit(
                    ModalUiUpdates.ShowToast(application.getString(R.string.quick_pick_confirm_logged_toast))
                )
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                analyticsLogger.logError(t)
                _uiUpdates.emit(
                    ModalUiUpdates.ShowToast(application.getString(R.string.quick_pick_confirm_log_failed_toast))
                )
            } finally {
                closeAll()
            }
        }
    }

    fun onQuickPickWidgetOpenDetailsTapped() {
        val dialog = _uiState.value.rootDialog as? DialogHandle.QuickPickWidgetConfirmDialog ?: return
        onTemplateDetailsButtonTapped(dialog.templateId)
    }

    /**
     * MediaStore ids of the gallery photos behind images added during this modal session,
     * keyed by persisted filename. Passed along on save so auto photo recognition knows
     * these photos are already logged.
     */
    private val pendingImageSources = mutableMapOf<String, Long>()

    private suspend fun persistImages(uris: List<Uri>): List<String> {
        val saved = uris.map { saveImageUseCase.execute(it) }
        saved.forEach { image -> image.sourceMediaStoreId?.let { pendingImageSources[image.filename] = it } }
        return saved.map { it.filename }
    }

    fun onImagesSelected(uris: List<Uri>) {
        runSafely("Couldn't add the selected images") {
            val existingCount = when (val root = _uiState.value.rootDialog) {
                is DialogHandle.RecordDetailsDialog.Edit -> root.imageFilenames.size
                is DialogHandle.RecordDetailsDialog.View -> if (root.isEditing) root.imageFilenames.size else 0
                else -> 0
            }
            if (existingCount + uris.size > MAX_IMAGES) {
                _uiUpdates.emit(ModalUiUpdates.ShowToast("Too many images selected. Maximum $MAX_IMAGES total."))
                if (_uiState.value.overlayDialog is DialogHandle.ImageInput) {
                    popOverlay()
                }
                return@runSafely
            }
            val persistedFilenames = persistImages(uris)

            when (val root = _uiState.value.rootDialog) {
                is DialogHandle.RecordDetailsDialog.Edit -> {
                    val updatedImages = root.imageFilenames + persistedFilenames
                    setRoot(root.copy(imageFilenames = updatedImages, recognisedFood = null))
                    recomputeHasUnsavedEdits()
                    runFoodRecognition(updatedImages)
                }

                is DialogHandle.RecordDetailsDialog.View -> {
                    if (!root.isEditing) return@runSafely
                    setRoot(root.copy(imageFilenames = root.imageFilenames + persistedFilenames))
                    recomputeHasUnsavedEdits()
                }

                else -> {
                    setRoot(
                        DialogHandle.RecordDetailsDialog.Edit(
                            title = TextFieldValue(),
                            titleHint = "Title",
                            description = TextFieldValue(),
                            imageFilenames = persistedFilenames,
                            recognisedFood = null,
                            pristineSnapshot = recordDetailsEditPristineSnapshot(
                                title = TextFieldValue(),
                                description = TextFieldValue(),
                                imageFilenames = persistedFilenames,
                            ),
                        )
                    )
                    runFoodRecognition(persistedFilenames)
                }
            }
        }
    }

    fun onImagesShared(uris: List<Uri>) {
        runSafely("Couldn't process the shared images") {
            if (uris.size > MAX_IMAGES) {
                _uiUpdates.emit(ModalUiUpdates.ShowToast("Too many images selected. Maximum $MAX_IMAGES total."))
                return@runSafely
            }
            val persistedFilenames = persistImages(uris)

            setRoot(
                DialogHandle.RecordDetailsDialog.Edit(
                    title = TextFieldValue(),
                    titleHint = "Title",
                    description = TextFieldValue(),
                    imageFilenames = persistedFilenames,
                    recognisedFood = null,
                    pristineSnapshot = recordDetailsEditPristineSnapshot(
                        title = TextFieldValue(),
                        description = TextFieldValue(),
                        imageFilenames = persistedFilenames,
                    ),
                )
            )
            runFoodRecognition(persistedFilenames)
        }
    }

    fun onNoImageSelected() {
        if (_uiState.value.overlayDialog is DialogHandle.ImageInput) {
            popOverlay()
        } else {
            closeAll()
        }
    }

    fun onRecordDetailsButtonTapped(recordId: Long) {
        openRecordDetails(recordId, edit = true)
    }

    fun onTemplateDetailsButtonTapped(templateId: Long) {
        runSafely("Couldn't open the entry") {
            recordDetailsJob?.cancel()
            recordDetailsJob = null
            val recordId = resolveFirstRecordIdForTemplateUseCase.execute(templateId) ?: return@runSafely
            val record = recordsRepository.get(recordId) ?: return@runSafely
            setRoot(buildEnrichedView(record = record, edit = false, templateDetailsMode = true))
        }
    }

    fun onRecordDetailsEditStarted() {
        updateRoot<DialogHandle.RecordDetailsDialog.View> {
            it.copy(isEditing = true)
        }
    }

    fun onRecordDetailsEditCancelled() {
        updateRoot<DialogHandle.RecordDetailsDialog.View> { v ->
            if (!v.isEditing) {
                v
            } else {
                val p = v.pristineSnapshot
                val title = p.title
                val desc = p.description
                v.copy(
                    isEditing = false,
                    title = TextFieldValue(title, selection = TextRange(title.length)),
                    description = TextFieldValue(desc, selection = TextRange(desc.length)),
                    imageFilenames = p.imageFilenames,
                    titleValidationError = null,
                )
            }
        }
    }

    fun onDialogDismissRequested(dialog: DialogHandle) {
        when {
            dialog === _uiState.value.overlayDialog -> {
                popOverlay()
            }

            _uiState.value.overlayDialog is DialogHandle.ImageInput -> {
                // Compose's Dialog fires onDismissRequest spuriously when the picker/camera
                // Activity steals focus from ModalActivity. Ignore — the overlay handles cleanup.
            }

            dialog is DialogHandle.RecordDetailsDialog.View && dialog.isEditing -> {
                onRecordDetailsEditCancelled()
            }

            dialog is DialogHandle.RecordDetailsDialog.View -> {
                closeAll()
            }

            dialog is DialogHandle.RecordDetailsDialog.Edit -> {
                runSafely("Couldn't discard the entry") {
                    dialog.imageFilenames.forEach { img ->
                        imageStore.delete(img)
                    }
                    closeAll()
                }
            }

            else -> {
                closeAll()
            }
        }
    }

    fun onTitleChanged(title: TextFieldValue) {
        val title = title.truncatedTo(256)
        val root = _uiState.value.rootDialog
        if (root is DialogHandle.RecordDetailsDialog.Edit && title.text.isNotBlank() && title.text != root.title.text) {
            if (recogniseFoodJob?.isActive == true) {
                recogniseFoodJob?.cancel()
                updateRoot<DialogHandle.RecordDetailsDialog.Edit> {
                    it.copy(showProgressIndicator = false, showRunAIButton = true)
                }
            }
        }
        updateRoot<DialogHandle.RecordDetailsDialog> {
            when (it) {
                is DialogHandle.RecordDetailsDialog.View ->
                    if (!it.isEditing) {
                        it
                    } else {
                        it.withTitle(title).withTitleValidationError(null)
                    }

                is DialogHandle.RecordDetailsDialog.Edit ->
                    it.withTitle(title).withTitleValidationError(null)
            }
        }
        recomputeHasUnsavedEdits()
    }

    fun onDescriptionChanged(description: TextFieldValue) {
        val description = description.truncatedTo(256)
        updateRoot<DialogHandle.RecordDetailsDialog> {
            when (it) {
                is DialogHandle.RecordDetailsDialog.View ->
                    if (!it.isEditing) it else it.withDescription(description)

                is DialogHandle.RecordDetailsDialog.Edit ->
                    it.withDescription(description)
            }
        }
        recomputeHasUnsavedEdits()
    }

    fun onImageTapped(imageFilename: String) {
        val root = _uiState.value.rootDialog
        val allImageFilenames = (root as? DialogHandle.RecordDetailsDialog)?.imageFilenames ?: listOf(imageFilename)
        val index = allImageFilenames.indexOf(imageFilename).coerceAtLeast(0)
        pushOverlay(DialogHandle.ViewImageDialog("", allImageFilenames, index))
    }

    fun onImageDownloadTapped(imageFilename: String) {
        if (photoExportJob?.isActive == true) return
        photoExportJob = runSafely("Couldn't save the image to your gallery") {
            _uiState.update { it.copy(photoExportInProgress = true) }
            try {
                val exportedUri = exportImageToGalleryUseCase.execute(imageFilename)
                _uiUpdates.emit(
                    ModalUiUpdates.ShowToast(
                        application.getString(
                            R.string.meal_details_photo_exported_toast,
                            ExportImageToGalleryUseCase.GALLERY_ALBUM_FOLDER,
                        ),
                    ),
                )
                _uiUpdates.emit(ModalUiUpdates.ShareImage(exportedUri))
            } finally {
                _uiState.update { it.copy(photoExportInProgress = false) }
            }
        }
    }

    fun onImageDeleteTapped(image: String) {
        updateRoot<DialogHandle.RecordDetailsDialog> {
            when (it) {
                is DialogHandle.RecordDetailsDialog.View ->
                    if (!it.isEditing) it else it.copy(imageFilenames = it.imageFilenames - image)

                is DialogHandle.RecordDetailsDialog.Edit -> it.copy(imageFilenames = it.imageFilenames - image)
            }
        }
        recomputeHasUnsavedEdits()
    }

    fun onImageMoveLeftTapped(image: String) {
        updateRoot<DialogHandle.RecordDetailsDialog> {
            val index = it.imageFilenames.indexOf(image)
            if (index <= 0) return@updateRoot it
            val newImages = it.imageFilenames.toMutableList().apply {
                add(index - 1, removeAt(index))
            }
            when (it) {
                is DialogHandle.RecordDetailsDialog.View ->
                    if (!it.isEditing) it else it.copy(imageFilenames = newImages)

                is DialogHandle.RecordDetailsDialog.Edit -> it.copy(imageFilenames = newImages)
            }
        }
        recomputeHasUnsavedEdits()
    }

    fun onImageMoveRightTapped(image: String) {
        updateRoot<DialogHandle.RecordDetailsDialog> {
            val index = it.imageFilenames.indexOf(image)
            if (index < 0 || index >= it.imageFilenames.lastIndex) return@updateRoot it
            val newImages = it.imageFilenames.toMutableList().apply {
                add(index + 1, removeAt(index))
            }
            when (it) {
                is DialogHandle.RecordDetailsDialog.View ->
                    if (!it.isEditing) it else it.copy(imageFilenames = newImages)

                is DialogHandle.RecordDetailsDialog.Edit -> it.copy(imageFilenames = newImages)
            }
        }
        recomputeHasUnsavedEdits()
    }

    fun onAddImageViaCameraTapped() {
        recogniseFoodJob?.cancel()
        pushOverlay(DialogHandle.ImageInput(type = ImageInputType.Camera))
    }

    fun onAddImageViaPickerTapped() {
        recogniseFoodJob?.cancel()
        pushOverlay(DialogHandle.ImageInput(type = ImageInputType.BrowseImages))
    }

    fun onSubmitButtonTapped() {
        val details = (_uiState.value.overlayDialog as? DialogHandle.RecordDetailsDialog)
            ?: (_uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog)
            ?: return
        if (details !is DialogHandle.RecordDetailsDialog.Edit) return
        runSafely("Couldn't save your entry") {
            handleCreateRecordDetailsSubmitted(details)
        }
    }

    fun onSaveDetailsTapped() {
        val details = (_uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.View) ?: return
        if (!details.isEditing) return
        runSafely("Couldn't save your changes") {
            handleEditRecordDialogSubmitted(details)
        }
    }

    fun onSaveAndAddDetailsTapped() {
        val details = (_uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.View) ?: return
        runSafely("Couldn't save your changes") {
            when {
                details.openedFromTemplateDetailsOnly ->
                    handleTemplateDetailsSaveAndAdd(details)

                else -> handleEditRecordSaveAndAdd(details)
            }
        }
    }

    fun onVariantTemplateSelected(templateId: Long) {
        runSafely("Couldn't switch to the selected variant") {
            val root = _uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.View ?: return@runSafely
            if (root.isEditing) return@runSafely
            if (templateId == root.templateDbId) return@runSafely
            if (root.hasUnsavedEdits) {
                pushOverlay(
                    DialogHandle.ConfirmSwitchTemplateDialog(pendingTemplateId = templateId),
                )
            } else {
                applyVariantTemplateSwitch(root, templateId)
            }
        }
    }

    fun onConfirmSwitchTemplateDespiteEdits() {
        val overlay = _uiState.value.overlayDialog as? DialogHandle.ConfirmSwitchTemplateDialog ?: return
        val root = _uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.View ?: return
        popOverlay()
        runSafely("Couldn't switch to the selected variant") {
            applyVariantTemplateSwitch(root, overlay.pendingTemplateId)
        }
    }

    fun onQuickPickStarToggled() {
        runSafely("Couldn't update your Quick Picks") {
            val root = _uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.View ?: return@runSafely
            if (root.isEditing) return@runSafely
            val templateId = root.templateDbId
            if (root.quickPickStarred) {
                applyQuickPickOverrideAndReloadWidgetUseCase.execute(
                    templateId,
                    Template.QuickPickOverride.EXCLUDE,
                )
            } else {
                applyQuickPickOverrideAndReloadWidgetUseCase.execute(
                    templateId,
                    Template.QuickPickOverride.INCLUDE,
                )
            }
            val template = recordsRepository.getTemplate(templateId)
            val starred = resolveQuickPickStarred(template)
            updateRoot<DialogHandle.RecordDetailsDialog.View> {
                it.copy(quickPickStarred = starred)
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
            runFoodRecognition(it.imageFilenames, withDelay = false)
            it.copy(
                title = TextFieldValue()
            )
        }
    }

    fun onEditTargetConfirmed(target: ChangeImagesTarget) {
        (_uiState.value.overlayDialog as? DialogHandle.EditTargetConfirmationDialog)
            ?.let {
                val recordId = it.recordId
                val imageFilenames = it.imageFilenames
                val title = it.title
                val description = it.description
                runSafely("Couldn't apply your changes") {
                    applyConfirmedSharedTemplateEditUseCase.execute(
                        target = target,
                        recordId = recordId,
                        imageFilenames = imageFilenames,
                        title = title,
                        description = description,
                    )
                    closeAll()
                }
            }
    }

    private fun runFoodRecognition(imageFilenames: List<String>, withDelay: Boolean = true) {
        recogniseFoodJob?.cancel()
        recogniseFoodJob = runSafely("Image recognition failed. Please try again later.") {
            if (withDelay) delay(1.3.seconds)
            updateRoot<DialogHandle.RecordDetailsDialog.Edit> {
                it.copy(showProgressIndicator = true)
            }
            try {
                val recognisedFood = foodRecognitionUseCase.execute(imageFilenames)
                updateRoot<DialogHandle.RecordDetailsDialog.Edit> { currentUiState ->
                    val title = currentUiState.title.takeIf { it.text.isNotBlank() }
                        ?: recognisedFood.title
                            ?.takeIf { it.isNotBlank() }
                            ?.let {
                                TextFieldValue(it, selection = TextRange(it.length))
                            }
                        ?: TextFieldValue()
                    recognisedFood.warning?.let {
                        _uiUpdates.emit(
                            ModalUiUpdates.Error(it)
                        )
                    }
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

    private fun openRecordDetails(recordId: Long, edit: Boolean) {
        recordDetailsJob?.cancel()
        recordDetailsJob = runSafely("Couldn't open the entry") {
            recordsRepository.get(recordId)?.let { record ->
                setRoot(buildEnrichedView(record, edit, templateDetailsMode = false))
            }
            recordsRepository.observe(recordId)
                .drop(1)
                .collect { record ->
                    setRoot(buildEnrichedView(record, edit, templateDetailsMode = false))
                }
        }
    }

    private suspend fun buildEnrichedView(
        record: Record,
        edit: Boolean,
        templateDetailsMode: Boolean,
    ): DialogHandle.RecordDetailsDialog.View {
        val existing = _uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.View
        val base = buildRecordDetailsViewDialogUseCase.execute(record, edit, templateDetailsMode)
        val variantList = listMealVariantsForTemplateUseCase.execute(record.template.dbId)
        val options = variantList?.takeIf { it.hasOtherVariants }
            ?.toPickerOptions(settingsRepository.getDiaryDayStartHour())
        val starred = resolveQuickPickStarred(record.template)
        val linkedCount = recordsRepository.countRecordsForTemplate(record.template.dbId)
        return if (existing != null && existing.recordId == record.recordId && existing.isEditing) {
            base.copy(
                variantPickerOptions = options,
                quickPickStarred = starred,
                linkedRecordCountForTemplate = linkedCount,
                isEditing = true,
                title = existing.title,
                description = existing.description,
                imageFilenames = existing.imageFilenames,
                imageRepresentativeFlags = existing.imageRepresentativeFlags,
                titleValidationError = existing.titleValidationError,
                pristineSnapshot = existing.pristineSnapshot,
            )
        } else {
            base.copy(
                variantPickerOptions = options,
                quickPickStarred = starred,
                linkedRecordCountForTemplate = linkedCount,
            )
        }
    }

    private suspend fun resolveQuickPickStarred(template: Template): Boolean {
        when (template.quickPickOverride) {
            Template.QuickPickOverride.EXCLUDE -> return false
            Template.QuickPickOverride.INCLUDE -> return true
            null -> {
                val picks = recordsRepository.getQuickPicks(QUICK_PICK_RANK_CHECK_COUNT)
                return picks.any { it.dbId == template.dbId }
            }
        }
    }

    /** True when the meal family has other logged variants (variant combo is shown). */
    private suspend fun isVariedTemplateFamily(details: DialogHandle.RecordDetailsDialog.View): Boolean {
        if (details.variantPickerOptions != null) return true
        val familyIds = recordsRepository.getTemplateIdsInSameVariantFamily(details.templateDbId)
        return familyIds.count { recordsRepository.countRecordsForTemplate(it) > 0 } > 1
    }

    private fun templateNeedsMacroAnalysis(template: Template): Boolean =
        template.isPending || template.nutrients.calories == null

    private suspend fun scheduleMacroAnalysisForRecordIfTemplateIncomplete(
        recordId: Long,
        templateId: Long,
    ) {
        val t = recordsRepository.getTemplate(templateId)
        if (templateNeedsMacroAnalysis(t)) {
            NutrientAnalysisWorker.setWorkRequest(
                appContext = application,
                recordId = recordId,
                force = true,
            )
        }
    }

    private suspend fun scheduleMacroAnalysisForAllRecordsUsingTemplate(templateId: Long) {
        recordsRepository.getRecordsByTemplate(templateId).forEach { record ->
            NutrientAnalysisWorker.setWorkRequest(
                appContext = application,
                recordId = record.recordId,
                force = true,
            )
        }
    }

    private suspend fun applyVariantTemplateSwitch(
        preserveMode: DialogHandle.RecordDetailsDialog.View,
        newTemplateId: Long,
    ) {
        val recordId = resolveFirstRecordIdForTemplateUseCase.execute(newTemplateId) ?: return
        val record = recordsRepository.get(recordId) ?: return
        val edit = preserveMode.allowEdit && !preserveMode.openedFromTemplateDetailsOnly
        val templateDetailsMode = preserveMode.openedFromTemplateDetailsOnly
        setRoot(buildEnrichedView(record, edit, templateDetailsMode))
    }

    private suspend fun handleCreateRecordDetailsSubmitted(
        dialogHandle: DialogHandle.RecordDetailsDialog.Edit,
    ) {
        val title = dialogHandle.title.text.trim()
        val description = dialogHandle.description.text.trim()
        val imageFilenames = dialogHandle.imageFilenames

        val result = validateCreateRecordUseCase.execute(
            imageFilenames = imageFilenames,
            title = title,
            description = description
        )

        when (result) {
            is CreateValidationResult.Error -> {
                applyValidationError(result.message)
            }

            is CreateValidationResult.Valid -> {
                val recordId = createRecordWithNewTemplateUseCase.execute(
                    imageFilenames = imageFilenames,
                    title = title,
                    description = description,
                    imageSourceMediaStoreIds = pendingImageSources.toMap(),
                )
                NutrientAnalysisWorker.setWorkRequest(
                    appContext = application,
                    recordId = recordId,
                    force = true,
                )
                closeAllRequestingNotificationPermission()
            }
        }
    }

    private suspend fun handleEditRecordDialogSubmitted(
        dialogHandle: DialogHandle.RecordDetailsDialog.View,
    ) {
        recordDetailsJob?.cancel()
        if (!dialogHandle.hasUnsavedEdits) {
            closeAll()
            return
        }
        val title = dialogHandle.title.text.trim()
        val description = dialogHandle.description.text.trim()

        val result = validateEditRecordUseCase.execute(
            recordId = dialogHandle.recordId,
            title = title,
            description = description,
        )
        when (result) {
            is EditValidationResult.Valid -> {
                val pristine = dialogHandle.pristineSnapshot
                val imagesNeedReanalysis = modalUiMapper.imagesRequireMacroReanalysis(
                    pristine.imageFilenames,
                    dialogHandle.imageFilenames,
                )
                val contentChanged = title != pristine.title.trim() ||
                        description != pristine.description.trim()
                val templateImages = dialogHandle.imageFilenames.map { filename ->
                    TemplateImageUpdate(
                        filename = filename,
                        isRepresentativeOfMeal = dialogHandle.imageRepresentativeFlags[filename],
                    )
                }
                if (isVariedTemplateFamily(dialogHandle)) {
                    recordsRepository.updateTemplate(
                        templateId = dialogHandle.templateDbId,
                        name = title,
                        description = description,
                        templateImages = templateImages,
                    )
                    if (imagesNeedReanalysis || contentChanged) {
                        scheduleMacroAnalysisForAllRecordsUsingTemplate(dialogHandle.templateDbId)
                    }
                } else {
                    val imagesReorderedOnly =
                        dialogHandle.imageFilenames != pristine.imageFilenames && !imagesNeedReanalysis
                    if (imagesReorderedOnly && !contentChanged) {
                        recordsRepository.updateTemplate(
                            templateId = dialogHandle.templateDbId,
                            name = title,
                            description = description,
                            templateImages = templateImages,
                        )
                    } else {
                        updateRecordWithNewTemplateUseCase.execute(
                            recordId = dialogHandle.recordId,
                            imageFilenames = dialogHandle.imageFilenames,
                            title = title,
                            description = description,
                            imageSourceMediaStoreIds = pendingImageSources.toMap(),
                        )
                        if (imagesNeedReanalysis || contentChanged) {
                            NutrientAnalysisWorker.setWorkRequest(
                                appContext = application,
                                recordId = dialogHandle.recordId,
                                force = true,
                            )
                        }
                    }
                }
                closeAll()
            }

            is EditValidationResult.Error -> {
                applyValidationError(result.message)
            }
        }
    }

    private suspend fun handleEditRecordSaveAndAdd(
        dialogHandle: DialogHandle.RecordDetailsDialog.View,
    ) {
        recordDetailsJob?.cancel()
        val title = dialogHandle.title.text.trim()
        val description = dialogHandle.description.text.trim()

        val result = validateEditRecordUseCase.execute(
            recordId = dialogHandle.recordId,
            title = title,
            description = description,
        )
        when (result) {
            is EditValidationResult.Valid -> {
                if (!dialogHandle.hasUnsavedEdits) {
                    val templateId = dialogHandle.templateDbId
                    val secondRecordId = createRecordFromTemplateUseCase.execute(templateId)
                    scheduleMacroAnalysisForRecordIfTemplateIncomplete(secondRecordId, templateId)
                } else {
                    val newTemplateId = createTemplateUseCase.execute(
                        imageFilenames = dialogHandle.imageFilenames,
                        title = title,
                        description = description,
                        parentTemplateId = dialogHandle.templateDbId,
                        imageSourceMediaStoreIds = pendingImageSources.toMap(),
                    )
                    val secondRecordId = createRecordFromTemplateUseCase.execute(newTemplateId)
                    NutrientAnalysisWorker.setWorkRequest(
                        appContext = application,
                        recordId = secondRecordId,
                        force = true,
                    )
                }
                closeAll()
            }

            is EditValidationResult.Error -> {
                applyValidationError(result.message)
            }
        }
    }

    private suspend fun handleTemplateDetailsSaveAndAdd(
        dialogHandle: DialogHandle.RecordDetailsDialog.View,
    ) {
        val title = dialogHandle.title.text.trim()
        val description = dialogHandle.description.text.trim()
        val imageFilenames = dialogHandle.imageFilenames
        val anchor = dialogHandle.variabilityAnchorTemplateDbId

        if (!dialogHandle.hasUnsavedEdits) {
            val recordId = createRecordFromTemplateUseCase.execute(anchor)
            scheduleMacroAnalysisForRecordIfTemplateIncomplete(recordId, anchor)
            closeAll()
            return
        }

        val result = validateCreateRecordUseCase.execute(
            imageFilenames = imageFilenames,
            title = title,
            description = description,
        )

        when (result) {
            is CreateValidationResult.Error -> {
                applyValidationError(result.message)
            }

            is CreateValidationResult.Valid -> {
                val recordId = createRecordWithNewTemplateUseCase.execute(
                    imageFilenames = imageFilenames,
                    title = title,
                    description = description,
                    parentTemplateId = anchor,
                    imageSourceMediaStoreIds = pendingImageSources.toMap(),
                )
                val templateId = recordsRepository.get(recordId)?.template?.dbId ?: return
                scheduleMacroAnalysisForRecordIfTemplateIncomplete(recordId, templateId)
                closeAll()
            }
        }
    }

    private fun applyValidationError(message: String?) {
        updateRoot<DialogHandle.RecordDetailsDialog> {
            it.withTitleValidationError(titleValidationError = message)
        }
    }

    private fun runSafely(defaultMessage: String, task: suspend () -> Unit): Job {
        return viewModelScope.launch(makeErrorHandler(defaultMessage)) {
            task()
        }
    }

    private fun makeErrorHandler(defaultMessage: String) = CoroutineExceptionHandler { _, exception ->
        if (exception is CancellationException) return@CoroutineExceptionHandler
        analyticsLogger.logError(exception)
        val message = when {
            exception is DomainError -> errorUiMapper.mapErrorMessage(exception, defaultMessage)
            else -> defaultMessage
        }
        viewModelScope.launch {
            _uiUpdates.emit(ModalUiUpdates.Error(message.ellipsize(300) ?: message))
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
        closeAllEmitting(ModalUiUpdates.Close)
    }

    private fun closeAllRequestingNotificationPermission() {
        closeAllEmitting(ModalUiUpdates.CloseAndRequestNotificationPermission)
    }

    private fun closeAllEmitting(update: ModalUiUpdates) {
        recogniseFoodJob?.cancel()
        recordDetailsJob?.cancel()
        photoExportJob?.cancel()
        _uiState.update {
            it.copy(
                rootDialog = null,
                overlayDialog = null,
                photoExportInProgress = false,
            )
        }
        viewModelScope.launch {
            _uiUpdates.emit(update)
        }
    }

    private fun emptyRecordDetailsEdit() = DialogHandle.RecordDetailsDialog.Edit(
        title = TextFieldValue(),
        titleHint = "Title",
        description = TextFieldValue(),
        imageFilenames = emptyList(),
        recognisedFood = null,
        showProgressIndicator = false,
        pristineSnapshot = recordDetailsEditPristineSnapshot(
            title = TextFieldValue(),
            description = TextFieldValue(),
            imageFilenames = emptyList(),
        ),
    )

    private fun recomputeHasUnsavedEdits() {
        updateRoot<DialogHandle.RecordDetailsDialog> {
            when (it) {
                is DialogHandle.RecordDetailsDialog.View -> it.copy(hasUnsavedEdits = modalUiMapper.hasUnsavedEdits(it))
                is DialogHandle.RecordDetailsDialog.Edit -> it.copy(hasUnsavedEdits = modalUiMapper.hasUnsavedEdits(it))
            }
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

private const val MAX_IMAGES = 5

private fun TextFieldValue.truncatedTo(maxLength: Int): TextFieldValue {
    if (text.length <= maxLength) return this
    val truncated = text.take(maxLength)
    return copy(
        text = truncated,
        selection = TextRange(selection.start.coerceAtMost(maxLength), selection.end.coerceAtMost(maxLength)),
    )
}
