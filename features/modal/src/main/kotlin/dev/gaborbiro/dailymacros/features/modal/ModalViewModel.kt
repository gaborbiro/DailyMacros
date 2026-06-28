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
import dev.gaborbiro.dailymacros.features.shared.NutrientAnalysisWorker
import dev.gaborbiro.dailymacros.features.modal.model.ChangeImagesTarget
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.ImageInputType
import dev.gaborbiro.dailymacros.features.modal.model.ModalUiState
import dev.gaborbiro.dailymacros.features.modal.model.ModalUiUpdates
import dev.gaborbiro.dailymacros.features.modal.model.hasUnsavedEdits
import dev.gaborbiro.dailymacros.features.modal.model.imagesRequireMacroReanalysis
import dev.gaborbiro.dailymacros.features.modal.model.recordDetailsEditPristineSnapshot
import dev.gaborbiro.dailymacros.features.modal.model.toPickerOptions
import dev.gaborbiro.dailymacros.features.modal.usecase.ApplyConfirmedSharedTemplateEditUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ApplyQuickPickOverrideAndReloadWidgetUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.BuildRecordDetailsViewDialogUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.EditValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodRecognitionUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.shared.ListMealVariantsForTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ResolveFirstRecordIdForTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.R
import dev.gaborbiro.dailymacros.features.modal.usecase.ExportImageToGalleryUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.UpdateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.features.shared.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import ellipsize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    fun onViewRecordImageDeeplinkReceived(recordId: Long) {
        runSafely {
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
        runSafely {
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

    fun onImagesSelected(uris: List<Uri>) {
        runSafely {
            val existingCount = when (val root = _uiState.value.rootDialog) {
                is DialogHandle.RecordDetailsDialog.Edit -> root.images.size
                is DialogHandle.RecordDetailsDialog.View -> if (root.isEditing) root.images.size else 0
                else -> 0
            }
            if (existingCount + uris.size > MAX_IMAGES) {
                _uiUpdates.emit(ModalUiUpdates.ShowToast("Too many images selected. Maximum $MAX_IMAGES total."))
                if (_uiState.value.overlayDialog is DialogHandle.ImageInput) {
                    popOverlay()
                }
                return@runSafely
            }
            val persistedFilenames = uris.map { saveImageUseCase.execute(it) }

            when (val root = _uiState.value.rootDialog) {
                is DialogHandle.RecordDetailsDialog.Edit -> {
                    val updatedImages = root.images + persistedFilenames
                    setRoot(root.copy(images = updatedImages, recognisedFood = null))
                    runFoodRecognition(updatedImages)
                }

                is DialogHandle.RecordDetailsDialog.View -> {
                    if (!root.isEditing) return@runSafely
                    setRoot(root.copy(images = root.images + persistedFilenames))
                }

                else -> {
                    setRoot(
                        DialogHandle.RecordDetailsDialog.Edit(
                            title = TextFieldValue(),
                            titleHint = "Title",
                            description = TextFieldValue(),
                            images = persistedFilenames,
                            recognisedFood = null,
                            pristineSnapshot = recordDetailsEditPristineSnapshot(
                                title = TextFieldValue(),
                                description = TextFieldValue(),
                                images = persistedFilenames,
                            ),
                        )
                    )
                    runFoodRecognition(persistedFilenames)
                }
            }
        }
    }

    fun onImagesShared(uris: List<Uri>) {
        runSafely {
            if (uris.size > MAX_IMAGES) {
                _uiUpdates.emit(ModalUiUpdates.ShowToast("Too many images selected. Maximum $MAX_IMAGES total."))
                return@runSafely
            }
            val persistedFilenames = uris.map { saveImageUseCase.execute(it) }

            setRoot(
                DialogHandle.RecordDetailsDialog.Edit(
                    title = TextFieldValue(),
                    titleHint = "Title",
                    description = TextFieldValue(),
                    images = persistedFilenames,
                    recognisedFood = null,
                    pristineSnapshot = recordDetailsEditPristineSnapshot(
                        title = TextFieldValue(),
                        description = TextFieldValue(),
                        images = persistedFilenames,
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
        runSafely {
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
                    images = p.images,
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
                runSafely {
                    dialog.images.forEach { img ->
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
    }

    fun onImageTapped(image: String) {
        val root = _uiState.value.rootDialog
        val allImages = (root as? DialogHandle.RecordDetailsDialog)?.images ?: listOf(image)
        val index = allImages.indexOf(image).coerceAtLeast(0)
        pushOverlay(DialogHandle.ViewImageDialog("", allImages, index))
    }

    fun onImageDownloadTapped(image: String) {
        if (photoExportJob?.isActive == true) return
        photoExportJob = runSafely {
            _uiState.update { it.copy(photoExportInProgress = true) }
            try {
                val exportedUri = exportImageToGalleryUseCase.execute(image)
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
                    if (!it.isEditing) it else it.copy(images = it.images - image)

                is DialogHandle.RecordDetailsDialog.Edit -> it.copy(images = it.images - image)
            }
        }
    }

    fun onImageMoveLeftTapped(image: String) {
        updateRoot<DialogHandle.RecordDetailsDialog> {
            val index = it.images.indexOf(image)
            if (index <= 0) return@updateRoot it
            val newImages = it.images.toMutableList().apply {
                add(index - 1, removeAt(index))
            }
            when (it) {
                is DialogHandle.RecordDetailsDialog.View ->
                    if (!it.isEditing) it else it.copy(images = newImages)

                is DialogHandle.RecordDetailsDialog.Edit -> it.copy(images = newImages)
            }
        }
    }

    fun onImageMoveRightTapped(image: String) {
        updateRoot<DialogHandle.RecordDetailsDialog> {
            val index = it.images.indexOf(image)
            if (index < 0 || index >= it.images.lastIndex) return@updateRoot it
            val newImages = it.images.toMutableList().apply {
                add(index + 1, removeAt(index))
            }
            when (it) {
                is DialogHandle.RecordDetailsDialog.View ->
                    if (!it.isEditing) it else it.copy(images = newImages)

                is DialogHandle.RecordDetailsDialog.Edit -> it.copy(images = newImages)
            }
        }
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
        runSafely {
            handleCreateRecordDetailsSubmitted(details)
        }
    }

    fun onSaveDetailsTapped() {
        val details = (_uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.View) ?: return
        if (!details.isEditing) return
        runSafely {
            handleEditRecordDialogSubmitted(details)
        }
    }

    fun onSaveAndAddDetailsTapped() {
        val details = (_uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.View) ?: return
        runSafely {
            when {
                details.openedFromTemplateDetailsOnly ->
                    handleTemplateDetailsSaveAndAdd(details)

                else -> handleEditRecordSaveAndAdd(details)
            }
        }
    }

    fun onVariantTemplateSelected(templateId: Long) {
        runSafely {
            val root = _uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.View ?: return@runSafely
            if (root.isEditing) return@runSafely
            if (templateId == root.templateDbId) return@runSafely
            if (root.hasUnsavedEdits()) {
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
        runSafely {
            applyVariantTemplateSwitch(root, overlay.pendingTemplateId)
        }
    }

    fun onQuickPickStarToggled() {
        runSafely {
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
            runFoodRecognition(it.images, withDelay = false)
            it.copy(
                title = TextFieldValue()
            )
        }
    }

    fun onEditTargetConfirmed(target: ChangeImagesTarget) {
        (_uiState.value.overlayDialog as? DialogHandle.EditTargetConfirmationDialog)
            ?.let {
                val recordId = it.recordId
                val images = it.images
                val title = it.title
                val description = it.description
                runSafely {
                    applyConfirmedSharedTemplateEditUseCase.execute(
                        target = target,
                        recordId = recordId,
                        images = images,
                        title = title,
                        description = description,
                    )
                    closeAll()
                }
            }
    }

    private fun runFoodRecognition(images: List<String>, withDelay: Boolean = true) {
        recogniseFoodJob?.cancel()
        recogniseFoodJob = runSafely {
            if (withDelay) delay(1500L)
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

    private fun openRecordDetails(recordId: Long, edit: Boolean) {
        recordDetailsJob?.cancel()
        recordDetailsJob = runSafely {
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
                images = existing.images,
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
                val recordId = createRecordWithNewTemplateUseCase.execute(
                    images = images,
                    title = title,
                    description = description,
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
        if (!dialogHandle.hasUnsavedEdits()) {
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
                val imagesNeedReanalysis = imagesRequireMacroReanalysis(
                    pristine.images,
                    dialogHandle.images,
                )
                val contentChanged = title != pristine.title.trim() ||
                    description != pristine.description.trim()
                val templateImages = dialogHandle.images.map { TemplateImageUpdate(filename = it) }
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
                        dialogHandle.images != pristine.images && !imagesNeedReanalysis
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
                            images = dialogHandle.images,
                            title = title,
                            description = description,
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
                if (!dialogHandle.hasUnsavedEdits()) {
                    val templateId = dialogHandle.templateDbId
                    val secondRecordId = createRecordFromTemplateUseCase.execute(templateId)
                    scheduleMacroAnalysisForRecordIfTemplateIncomplete(secondRecordId, templateId)
                } else {
                    val newTemplateId = createTemplateUseCase.execute(
                        images = dialogHandle.images,
                        title = title,
                        description = description,
                        parentTemplateId = dialogHandle.templateDbId,
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
        val images = dialogHandle.images
        val anchor = dialogHandle.variabilityAnchorTemplateDbId

        if (!dialogHandle.hasUnsavedEdits()) {
            val recordId = createRecordFromTemplateUseCase.execute(anchor)
            scheduleMacroAnalysisForRecordIfTemplateIncomplete(recordId, anchor)
            closeAll()
            return
        }

        val result = validateCreateRecordUseCase.execute(
            images = images,
            title = title,
            description = description,
        )

        when (result) {
            is CreateValidationResult.Error -> {
                applyValidationError(result.message)
            }

            is CreateValidationResult.Valid -> {
                val recordId = createRecordWithNewTemplateUseCase.execute(
                    images = images,
                    title = title,
                    description = description,
                    parentTemplateId = anchor,
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

    private fun runSafely(task: suspend () -> Unit): Job {
        return viewModelScope.launch(errorHandler) {
            task()
        }
    }

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        if (exception is CancellationException) return@CoroutineExceptionHandler
        analyticsLogger.logError(exception)
        val message = when {
            exception is ChatGPTDomainError -> modalUiMapper.mapDomainErrorToUserMessage(exception)
            else -> exception.message ?: exception.cause?.message
        }
        viewModelScope.launch {
            _uiUpdates.emit(
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
        images = emptyList(),
        recognisedFood = null,
        showProgressIndicator = false,
        pristineSnapshot = recordDetailsEditPristineSnapshot(
            title = TextFieldValue(),
            description = TextFieldValue(),
            images = emptyList(),
        ),
    )

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
