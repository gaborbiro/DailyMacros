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
import dev.gaborbiro.dailymacros.features.modal.model.CloseSignal
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.ImageInputType
import dev.gaborbiro.dailymacros.features.modal.model.ModalUiState
import dev.gaborbiro.dailymacros.features.modal.model.ModalUiUpdates
import dev.gaborbiro.dailymacros.features.modal.model.recordDetailsEditPristineSnapshot
import dev.gaborbiro.dailymacros.features.modal.model.toPickerOptions
import dev.gaborbiro.dailymacros.features.modal.usecase.ApplyQuickPickOverrideAndReloadWidgetUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.BuildRecordDetailsViewDialogUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.LogQuickPickMealUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.EditValidationResult
import dev.gaborbiro.dailymacros.features.modal.usecase.ExportImageToGalleryUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodRecognitionUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ResolveFirstRecordIdForTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveRecordEditsUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ScheduleAnalysisIfTemplateIncompleteUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.features.shared.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.shared.ErrorUiMapper
import dev.gaborbiro.dailymacros.features.shared.ListMealVariantsForTemplateUseCase
import dev.gaborbiro.dailymacros.features.shared.NutrientAnalysisWorker
import dev.gaborbiro.dailymacros.features.shared.UserFacingError
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealReminderRefresher
import dev.gaborbiro.dailymacros.repositories.common.model.toZonedInstant
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
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
import java.time.ZoneId
import java.time.ZonedDateTime
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
    private val saveRecordEditsUseCase: SaveRecordEditsUseCase,
    private val scheduleAnalysisIfTemplateIncompleteUseCase: ScheduleAnalysisIfTemplateIncompleteUseCase,
    private val validateEditRecordUseCase: ValidateEditRecordUseCase,
    private val validateCreateRecordUseCase: ValidateCreateRecordUseCase,
    private val saveImageUseCase: SaveImageUseCase,
    private val exportImageToGalleryUseCase: ExportImageToGalleryUseCase,
    private val getRecordImageUseCase: GetRecordImageUseCase,
    private val getTemplateImageUseCase: GetTemplateImageUseCase,
    private val foodRecognitionUseCase: FoodRecognitionUseCase,
    private val applyQuickPickOverrideAndReloadWidgetUseCase: ApplyQuickPickOverrideAndReloadWidgetUseCase,
    private val logQuickPickMealUseCase: LogQuickPickMealUseCase,
    private val analyticsLogger: AnalyticsLogger,
    private val errorUiMapper: ErrorUiMapper,
    private val mealReminderRefresher: MealReminderRefresher,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ModalUiState())
    val uiState: StateFlow<ModalUiState> = _uiState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<ModalUiUpdates>(extraBufferCapacity = 16)
    val uiUpdates: SharedFlow<ModalUiUpdates> = _uiUpdates.asSharedFlow()

    private var recogniseFoodJob: Job? = null
    private var photoExportJob: Job? = null

    private var recordDetailsJob: Job? = null

    /**
     * A re-analysis the user started from the open details dialog. The worker only marks the meal as
     * pending once it picks the job up, so until then (and while it runs) the dialog keeps showing
     * "Analyzing…" instead of the stale or empty results in between.
     */
    private var reanalysisProgress = ReanalysisProgress.NONE

    private enum class ReanalysisProgress { NONE, REQUESTED, RUNNING }

    private companion object {
        /** Matches widget reload ranking window (see ReloadWorker). */
        const val QUICK_PICK_RANK_CHECK_COUNT = 30
    }

    fun onCreateRecordWithCameraDeeplinkReceived() {
        setRoot(newRecordDetailsEdit())
        pushOverlay(DialogHandle.ImageInput(type = ImageInputType.Camera))
    }

    fun onCreateRecordWithBrowseImagesDeeplinkReceived() {
        setRoot(newRecordDetailsEdit())
        pushOverlay(DialogHandle.ImageInput(type = ImageInputType.BrowseImages))
    }

    fun onCreateRecordWithTextDeeplinkReceived() {
        setRoot(newRecordDetailsEdit())
    }

    fun onPhotoRecognitionDetailsDeeplinkReceived(recognisedTitle: String, imageFilename: String) {
        setRoot(
            newRecordDetailsEdit(
                title = TextFieldValue(recognisedTitle, TextRange(recognisedTitle.length)),
                imageFilenames = listOf(imageFilename),
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
                    closeAll()
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
                    closeAll()
                }
        }
    }

    fun onViewRecordDetailsDeeplinkReceived(recordId: Long) {
        openRecordDetails(recordId, edit = true)
    }

    /**
     * With the confirmation turned off, this logs the meal and closes before returning, so
     * ModalActivity can finish before its window is ever drawn.
     */
    fun onQuickPickWidgetConfirmDeeplinkReceived(templateId: Long, templateName: String) {
        if (settingsRepository.getQuickPickConfirmationEnabled().not()) {
            logQuickPickMeal(templateId)
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
        logQuickPickMeal(dialog.templateId)
    }

    fun onQuickPickWidgetDontShowAgainChanged(dontShowAgain: Boolean) {
        val dialog = _uiState.value.rootDialog as? DialogHandle.QuickPickWidgetConfirmDialog ?: return
        settingsRepository.setQuickPickConfirmationEnabled(dontShowAgain.not())
        _uiState.update { it.copy(rootDialog = dialog.copy(dontShowAgain = dontShowAgain)) }
    }

    /** The meal is logged (and confirmed with a toast) after this screen has closed. */
    private fun logQuickPickMeal(templateId: Long) {
        logQuickPickMealUseCase.execute(templateId)
        closeAll()
    }

    fun onQuickPickWidgetOpenDetailsTapped() {
        val dialog = _uiState.value.rootDialog as? DialogHandle.QuickPickWidgetConfirmDialog ?: return
        onTemplateDetailsButtonTapped(dialog.templateId)
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
            val persistedFilenames = uris.map { saveImageUseCase.execute(it) }

            when (val root = _uiState.value.rootDialog) {
                is DialogHandle.RecordDetailsDialog.Edit -> {
                    val updatedImages = root.imageFilenames + persistedFilenames
                    val titleAlreadySet = root.title.text.isNotBlank()
                    setRoot(
                        root.copy(
                            imageFilenames = updatedImages,
                            recognisedFood = if (titleAlreadySet) root.recognisedFood else null,
                        )
                    )
                    recomputeHasUnsavedEdits()
                    if (!titleAlreadySet) {
                        runFoodRecognition(updatedImages)
                    }
                }

                is DialogHandle.RecordDetailsDialog.View -> {
                    if (!root.isEditing) return@runSafely
                    setRoot(root.copy(imageFilenames = root.imageFilenames + persistedFilenames))
                    recomputeHasUnsavedEdits()
                }

                else -> {
                    setRoot(newRecordDetailsEdit(imageFilenames = persistedFilenames))
                    runFoodRecognition(persistedFilenames)
                }
            }
        }
    }

    fun onImagesShared(uris: List<Uri>) {
        runSafely("Couldn't process the shared images") {
            val existingCount = when (val root = _uiState.value.rootDialog) {
                is DialogHandle.RecordDetailsDialog.Edit -> root.imageFilenames.size
                is DialogHandle.RecordDetailsDialog.View -> if (root.isEditing) root.imageFilenames.size else 0
                else -> 0
            }
            if (existingCount + uris.size > MAX_IMAGES) {
                _uiUpdates.emit(ModalUiUpdates.ShowToast("Too many images selected. Maximum $MAX_IMAGES total."))
                return@runSafely
            }
            val persistedFilenames = uris.map { saveImageUseCase.execute(it) }

            when (val root = _uiState.value.rootDialog) {
                is DialogHandle.RecordDetailsDialog.Edit -> {
                    val updatedImages = root.imageFilenames + persistedFilenames
                    val titleAlreadySet = root.title.text.isNotBlank()
                    setRoot(
                        root.copy(
                            imageFilenames = updatedImages,
                            recognisedFood = if (titleAlreadySet) root.recognisedFood else null,
                        )
                    )
                    recomputeHasUnsavedEdits()
                    if (!titleAlreadySet) {
                        runFoodRecognition(updatedImages)
                    }
                }

                is DialogHandle.RecordDetailsDialog.View -> {
                    if (!root.isEditing) {
                        setRoot(newRecordDetailsEdit(imageFilenames = persistedFilenames))
                        runFoodRecognition(persistedFilenames)
                    } else {
                        setRoot(root.copy(imageFilenames = root.imageFilenames + persistedFilenames))
                        recomputeHasUnsavedEdits()
                    }
                }

                else -> {
                    setRoot(newRecordDetailsEdit(imageFilenames = persistedFilenames))
                    runFoodRecognition(persistedFilenames)
                }
            }
        }
    }

    fun onNoImageSelected() {
        if (_uiState.value.overlayDialog is DialogHandle.ImageInput) {
            popOverlay()
            (_uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.Edit)?.let { edit ->
                // Auto-recognition only ever runs for the first photo. Tapping an add-photo button
                // cancels it as a signal that a second photo is coming; if the user backs out of
                // that without picking one, the first photo's recognition is left interrupted with
                // no button shown - resume it here. Backing out of a *third+* photo should do
                // nothing: by then recognition already ran its course and the AI button is available.
                if (edit.imageFilenames.size == 1 && !edit.showRunAIButton && !edit.showProgressIndicator) {
                    runFoodRecognition(edit.imageFilenames)
                }
            }
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
            it.copy(isEditing = true, editStartedAt = ZonedDateTime.now(ZoneId.systemDefault()))
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
                    hasUnsavedEdits = false,
                    editsRequireAnalysis = false,
                    editStartedAt = null,
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
        val description = description.truncatedTo(MAX_DESCRIPTION_LENGTH)
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

    fun onCorrectionChanged(correction: TextFieldValue) {
        updateRoot<DialogHandle.RecordDetailsDialog.View> {
            if (it.isEditing || !it.allowCorrection) {
                it
            } else {
                val capacity = MealEditRules.correctionCapacity(it.pristineSnapshot.description, MAX_DESCRIPTION_LENGTH)
                val truncated = correction.truncatedTo(capacity)
                it.copy(correction = truncated, canReanalyze = truncated.text.isNotBlank())
            }
        }
    }

    // Dictation appends to what's already typed, then takes the same path as typing, so
    // truncation, validation and unsaved-edit tracking stay in one place.

    fun onTitleSpoken(spoken: String) {
        val root = _uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog ?: return
        onTitleChanged(appendSpokenText(root.title, spoken))
    }

    fun onDescriptionSpoken(spoken: String) {
        val root = _uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog ?: return
        onDescriptionChanged(appendSpokenText(root.description, spoken))
    }

    fun onCorrectionSpoken(spoken: String) {
        val root = _uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.View ?: return
        onCorrectionChanged(appendSpokenText(root.correction, spoken))
    }

    fun onVoiceInputFailed(error: Throwable) {
        val message = application.getString(R.string.modal_content_voice_input_unavailable_toast)
        analyticsLogger.logError(UserFacingError(message, error))
        viewModelScope.launch { _uiUpdates.emit(ModalUiUpdates.ShowToast(message)) }
    }

    /**
     * Folds the correction into the meal's description and sends it back to the AI, keeping the dialog
     * open: the record is observed, so the new results replace the old ones in place.
     */
    fun onReanalyzeTapped() {
        val root = _uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.View ?: return
        if (root.isEditing || !root.allowCorrection || !root.canReanalyze) return
        val pristine = root.pristineSnapshot
        val description = MealEditRules.appendCorrection(pristine.description, root.correction.text)
        reanalysisProgress = ReanalysisProgress.REQUESTED
        updateRoot<DialogHandle.RecordDetailsDialog.View> {
            it.copy(correction = TextFieldValue(), canReanalyze = false, showLoadingIndicator = true)
        }
        runSafely("Couldn't re-analyze your meal") {
            try {
                persistRecordEdits(
                    dialogHandle = root,
                    title = pristine.title.trim(),
                    description = description,
                )
            } catch (t: Throwable) {
                reanalysisProgress = ReanalysisProgress.NONE
                updateRoot<DialogHandle.RecordDetailsDialog.View> {
                    it.copy(correction = root.correction, canReanalyze = true, showLoadingIndicator = false)
                }
                throw t
            }
        }
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
                        "\n\nLogging the meal is more important than the exact time — you can always fix the date and time afterwards."
            )
        )
    }

    fun onEditTimestampTapped() {
        val root = _uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.View ?: return
        if (!root.allowEdit) return
        pushOverlay(DialogHandle.EditTimestampDialog(recordId = root.recordId, timestamp = root.timestamp))
    }

    fun onTimestampPicked(timestamp: ZonedDateTime) {
        val overlay = _uiState.value.overlayDialog as? DialogHandle.EditTimestampDialog ?: return
        popOverlay()
        runSafely("Couldn't update the date and time") {
            val record = recordsRepository.get(overlay.recordId) ?: return@runSafely
            recordsRepository.updateRecord(record.copy(timestamp = timestamp.toZonedInstant()))
            // Moving a meal can move it into another slot (dinner logged, then dated to lunch).
            mealReminderRefresher.refresh(trigger = "timestamp edit")
            updateRoot<DialogHandle.RecordDetailsDialog.View> {
                if (it.recordId == overlay.recordId) it.copy(timestamp = timestamp) else it
            }
        }
    }

    fun onRunAIButtonTapped() {
        updateRoot<DialogHandle.RecordDetailsDialog.Edit> {
            runFoodRecognition(it.imageFilenames, withDelay = false)
            it.copy(
                title = TextFieldValue()
            )
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
        reanalysisProgress = ReanalysisProgress.NONE
        recordDetailsJob = runSafely("Couldn't open the entry") {
            recordsRepository.get(recordId)?.let { record ->
                setRoot(buildEnrichedView(record, edit, templateDetailsMode = false))
            }
            recordsRepository.observe(recordId)
                .drop(1)
                .collect { record ->
                    // null: recordId doesn't/no longer exists (e.g. a stale
                    // widget row after a restore swapped in a different
                    // database) - nothing to update the dialog with.
                    record?.let {
                        trackReanalysisProgress(it.template.isPending)
                        setRoot(buildEnrichedView(it, edit, templateDetailsMode = false))
                    }
                }
        }
    }

    private fun trackReanalysisProgress(templatePending: Boolean) {
        reanalysisProgress = when (reanalysisProgress) {
            ReanalysisProgress.REQUESTED -> if (templatePending) ReanalysisProgress.RUNNING else ReanalysisProgress.REQUESTED
            ReanalysisProgress.RUNNING -> if (templatePending) ReanalysisProgress.RUNNING else ReanalysisProgress.NONE
            ReanalysisProgress.NONE -> ReanalysisProgress.NONE
        }
    }

    private suspend fun buildEnrichedView(
        record: Record,
        edit: Boolean,
        templateDetailsMode: Boolean,
    ): DialogHandle.RecordDetailsDialog.View {
        val existing = _uiState.value.rootDialog as? DialogHandle.RecordDetailsDialog.View
        val base = buildRecordDetailsViewDialogUseCase.execute(record, edit, templateDetailsMode)
        val diaryDayStartHour = settingsRepository.getDiaryDayStartHour()
        val variantList = listMealVariantsForTemplateUseCase.execute(record.template.dbId)
        val options = variantList?.takeIf { it.hasOtherVariants }
            ?.toPickerOptions(diaryDayStartHour)
        val starred = resolveQuickPickStarred(record.template)
        val linkedCount = recordsRepository.countRecordsForTemplate(record.template.dbId)
        val sameRecord = existing != null && existing.recordId == record.recordId
        val enriched = if (sameRecord && existing.isEditing) {
            base.copy(
                variantPickerOptions = options,
                quickPickStarred = starred,
                linkedRecordCountForTemplate = linkedCount,
                diaryDayStartHour = diaryDayStartHour,
                isEditing = true,
                title = existing.title,
                description = existing.description,
                imageFilenames = existing.imageFilenames,
                imageRepresentativeFlags = existing.imageRepresentativeFlags,
                titleValidationError = existing.titleValidationError,
                pristineSnapshot = existing.pristineSnapshot,
                editsRequireAnalysis = existing.editsRequireAnalysis,
            )
        } else {
            base.copy(
                variantPickerOptions = options,
                quickPickStarred = starred,
                linkedRecordCountForTemplate = linkedCount,
                diaryDayStartHour = diaryDayStartHour,
            )
        }
        return enriched.copy(
            // A half-typed correction survives unrelated updates of the same meal (e.g. a star toggle).
            correction = if (sameRecord) existing.correction else TextFieldValue(),
            canReanalyze = sameRecord && existing.canReanalyze,
            showLoadingIndicator = enriched.showLoadingIndicator || reanalysisProgress != ReanalysisProgress.NONE,
        )
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
                    timestamp = dialogHandle.startedAt,
                )
                NutrientAnalysisWorker.setWorkRequest(
                    appContext = application,
                    recordId = recordId,
                    force = true,
                    wifiOnly = false,
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
                persistRecordEdits(dialogHandle, title, description)
                closeAll()
            }

            is EditValidationResult.Error -> {
                applyValidationError(result.message)
            }
        }
    }

    /** Saves [title] and [description], with the dialog's current photos, to the meal. */
    private suspend fun persistRecordEdits(
        dialogHandle: DialogHandle.RecordDetailsDialog.View,
        title: String,
        description: String,
    ) {
        saveRecordEditsUseCase.execute(
            recordId = dialogHandle.recordId,
            templateId = dialogHandle.templateDbId,
            pristine = dialogHandle.pristineSnapshot,
            knownToHaveLoggedVariants = dialogHandle.variantPickerOptions != null,
            title = title,
            description = description,
            imageFilenames = dialogHandle.imageFilenames,
            imageRepresentativeFlags = dialogHandle.imageRepresentativeFlags,
        )
    }

    private suspend fun handleEditRecordSaveAndAdd(
        dialogHandle: DialogHandle.RecordDetailsDialog.View,
    ) {
        recordDetailsJob?.cancel()

        // No edits: just log the meal again from its existing template. This must not require a
        // title — an as-yet-unrecognised meal has a blank title, and "Log meal again" simply
        // duplicates the entry (macro analysis is rescheduled if the template is still incomplete).
        if (!dialogHandle.hasUnsavedEdits) {
            val templateId = dialogHandle.templateDbId
            val secondRecordId = createRecordFromTemplateUseCase.execute(
                templateId,
                ZonedDateTime.now(ZoneId.systemDefault()),
            )
            scheduleAnalysisIfTemplateIncompleteUseCase.execute(secondRecordId, templateId)
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
                val newTemplateId = createTemplateUseCase.execute(
                    imageFilenames = dialogHandle.imageFilenames,
                    title = title,
                    description = description,
                    parentTemplateId = dialogHandle.templateDbId,
                )
                val secondRecordId = createRecordFromTemplateUseCase.execute(
                    newTemplateId,
                    dialogHandle.editStartedAt ?: ZonedDateTime.now(ZoneId.systemDefault()),
                )
                NutrientAnalysisWorker.setWorkRequest(
                    appContext = application,
                    recordId = secondRecordId,
                    force = true,
                    wifiOnly = false,
                )
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
            val recordId = createRecordFromTemplateUseCase.execute(
                anchor,
                ZonedDateTime.now(ZoneId.systemDefault()),
            )
            scheduleAnalysisIfTemplateIncompleteUseCase.execute(recordId, anchor)
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
                    timestamp = dialogHandle.editStartedAt ?: ZonedDateTime.now(ZoneId.systemDefault()),
                    parentTemplateId = anchor,
                )
                val templateId = recordsRepository.get(recordId)?.template?.dbId ?: return
                scheduleAnalysisIfTemplateIncompleteUseCase.execute(recordId, templateId)
                closeAll()
            }
        }
    }

    private fun applyValidationError(message: String?) {
        updateRoot<DialogHandle.RecordDetailsDialog> {
            it.withTitleValidationError(titleValidationError = message)
        }
    }

    private fun runSafely(genericMessage: String, task: suspend () -> Unit): Job {
        return viewModelScope.launch(makeErrorHandler(genericMessage)) {
            task()
        }
    }

    private fun makeErrorHandler(genericMessage: String) = CoroutineExceptionHandler { _, exception ->
        if (exception is CancellationException) return@CoroutineExceptionHandler
        val userFacingError = errorUiMapper.map(exception, genericMessage)
        analyticsLogger.logError(userFacingError)
        val message = userFacingError.message
        viewModelScope.launch {
            _uiUpdates.emit(ModalUiUpdates.Error(message.ellipsize(300) ?: message))
        }
    }

    private fun setRoot(dialog: DialogHandle) {
        _uiState.update {
            it.copy(rootDialog = dialog, overlayDialog = null, closeSignal = null)
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
        closeAllWithSignal(CloseSignal.CLOSE)
    }

    private fun closeAllRequestingNotificationPermission() {
        closeAllWithSignal(CloseSignal.CLOSE_AND_REQUEST_NOTIFICATION_PERMISSION)
    }

    private fun closeAllWithSignal(signal: CloseSignal) {
        recogniseFoodJob?.cancel()
        recordDetailsJob?.cancel()
        photoExportJob?.cancel()
        reanalysisProgress = ReanalysisProgress.NONE
        _uiState.update {
            it.copy(
                rootDialog = null,
                overlayDialog = null,
                photoExportInProgress = false,
                closeSignal = signal,
            )
        }
    }

    /**
     * A create-record dialog, optionally opened with content (a shared photo, a recognised title).
     * Its baseline is always empty, so whatever it opens with counts as unsaved and cancelling asks
     * before discarding it, just like content added after opening.
     */
    private fun newRecordDetailsEdit(
        title: TextFieldValue = TextFieldValue(),
        imageFilenames: List<String> = emptyList(),
    ): DialogHandle.RecordDetailsDialog.Edit {
        val edit = DialogHandle.RecordDetailsDialog.Edit(
            title = title,
            titleHint = "Title (optional)",
            description = TextFieldValue(),
            imageFilenames = imageFilenames,
            recognisedFood = null,
            showProgressIndicator = false,
            pristineSnapshot = recordDetailsEditPristineSnapshot(
                title = TextFieldValue(),
                description = TextFieldValue(),
                imageFilenames = emptyList(),
            ),
            startedAt = ZonedDateTime.now(ZoneId.systemDefault()),
        )
        return edit.copy(hasUnsavedEdits = modalUiMapper.hasUnsavedEdits(edit))
    }

    private fun recomputeHasUnsavedEdits() {
        updateRoot<DialogHandle.RecordDetailsDialog> {
            when (it) {
                is DialogHandle.RecordDetailsDialog.View -> it.copy(
                    hasUnsavedEdits = modalUiMapper.hasUnsavedEdits(it),
                    editsRequireAnalysis = MealEditRules.editsRequireMacroAnalysis(
                        pristine = it.pristineSnapshot,
                        title = it.title.text,
                        description = it.description.text,
                        imageFilenames = it.imageFilenames,
                    ),
                )
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
private const val MAX_DESCRIPTION_LENGTH = 256

private fun TextFieldValue.truncatedTo(maxLength: Int): TextFieldValue {
    if (text.length <= maxLength) return this
    val truncated = text.take(maxLength)
    return copy(
        text = truncated,
        selection = TextRange(selection.start.coerceAtMost(maxLength), selection.end.coerceAtMost(maxLength)),
    )
}
