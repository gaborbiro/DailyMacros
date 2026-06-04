package dev.gaborbiro.dailymacros.features.modal

import android.app.ComponentCaller
import android.app.KeyguardManager
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.data.file.domain.FileStore
import dev.gaborbiro.dailymacros.data.image.DefaultFoodPicExt
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.data.file.di.FileStorePublicBucketEphemeral
import dev.gaborbiro.dailymacros.features.common.views.InfoDialog
import dev.gaborbiro.dailymacros.features.common.views.LocalImageStore
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.ImageInputType
import dev.gaborbiro.dailymacros.features.modal.model.ModalUiUpdates
import dev.gaborbiro.dailymacros.features.modal.model.ModalUiUpdates.ShareImage
import dev.gaborbiro.dailymacros.features.modal.views.EditTargetConfirmationDialog
import dev.gaborbiro.dailymacros.features.modal.views.ImageDialog
import dev.gaborbiro.dailymacros.features.modal.views.ConfirmSwitchTemplateDialog
import dev.gaborbiro.dailymacros.features.modal.views.RecordDetailsDialog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class ModalActivity : AppCompatActivity() {

    @Inject
    lateinit var imageStore: ImageStore

    @Inject
    @FileStorePublicBucketEphemeral
    lateinit var cacheFileStore: FileStore

    private val viewModel: ModalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (km.isKeyguardLocked) {
            km.requestDismissKeyguard(this, null)
        }

        getImagesFromIntent().takeIf { it.isNotEmpty() }
            ?.let {
                viewModel.onImagesShared(it)
            }
            ?: run {
                getActionFromIntent()
                    ?.let(::handleAction)
                    ?: run {
                        finish()
                        return
                    }
            }

        setContent {
            val viewState by viewModel.uiState.collectAsStateWithLifecycle()

            AppTheme {
                CompositionLocalProvider(LocalImageStore provides imageStore) {
                    val errorMessages = remember {
                        viewModel.uiUpdates
                            .filterIsInstance<ModalUiUpdates.Error>()
                            .map { it.message }
                    }
                    listOfNotNull(viewState.rootDialog, viewState.overlayDialog).forEach {
                        Dialog(
                            dialogHandle = it,
                            errorMessages = errorMessages,
                        )
                    }
                }
            }

            LaunchedEffect(viewModel) {
                viewModel.uiUpdates
                    .filterIsInstance<ModalUiUpdates.Close>()
                    .collect { finish() }
            }

            LaunchedEffect(viewModel) {
                viewModel.uiUpdates
                    .filterIsInstance<ShareImage>()
                    .collect { update ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, update.uri)
                            addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(
                            Intent.createChooser(
                                shareIntent,
                                getString(R.string.meal_details_photo_share_chooser_title),
                            ),
                        )
                    }
            }
        }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        getImagesFromIntent(intent).takeIf { it.isNotEmpty() }
            ?.let {
                viewModel.onImagesShared(it)
            }
            ?: run {
                val action = getActionFromIntent(intent)
                action?.let {
                    handleAction(action, intent)
                }
            }
    }

    private fun getActionFromIntent(intent: Intent = this.intent): Action? {
        val action = intent.getStringExtra(EXTRA_ACTION)
            ?.let { Action.valueOf(it) }
        intent.removeExtra(EXTRA_ACTION) // consume intent
        return action
    }

    private fun getImagesFromIntent(intent: Intent = this.intent): List<Uri> {
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                listOfNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM))
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM) ?: emptyList()
            }

            else -> emptyList()
        }
    }

    private fun handleAction(action: Action, intent: Intent = this.intent) {
        when (action) {
            Action.CAMERA -> viewModel.onCreateRecordWithCameraDeeplinkReceived()
            Action.BROWSE_IMAGES -> viewModel.onCreateRecordWithBrowseImagesDeeplinkReceived()
            Action.TEXT_ONLY -> viewModel.onCreateRecordWithTextDeeplinkReceived()

            Action.VIEW_RECORD_DETAILS -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                viewModel.onViewRecordDetailsDeeplinkReceived(recordId)
            }

            Action.VIEW_IMAGE -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                if (recordId != -1L) {
                    viewModel.onViewRecordImageDeeplinkReceived(recordId)
                } else {
                    val templateId = intent.getLongExtra(EXTRA_TEMPLATE_ID, -1L)
                    viewModel.onViewTemplateImageDeeplinkReceived(templateId)
                }
            }

            Action.VIEW_TEMPLATE_DETAILS -> {
                val templateId = intent.getLongExtra(EXTRA_TEMPLATE_ID, -1L)
                viewModel.onViewTemplateDetailsDeeplinkReceived(templateId)
            }
        }
    }

    @Composable
    fun Dialog(
        dialogHandle: DialogHandle?,
        errorMessages: Flow<String>,
    ) {
        val onDismissRequested: () -> Unit =
            remember(dialogHandle) {
                {
                    dialogHandle
                        ?.let { viewModel.onDialogDismissRequested(it) }
                }
            }
        when (dialogHandle) {
            is DialogHandle.RecordDetailsDialog -> RecordDetailsDialog(
                dialogHandle = dialogHandle,
                errorMessages = errorMessages,
                onSubmitButtonTapped = viewModel::onSubmitButtonTapped,
                onSaveDetailsTapped = viewModel::onSaveDetailsTapped,
                onSaveAndAddDetailsTapped = viewModel::onSaveAndAddDetailsTapped,
                onTitleChanged = viewModel::onTitleChanged,
                onDescriptionChanged = viewModel::onDescriptionChanged,
                onImageTapped = viewModel::onImageTapped,
                onImageDownloadTapped = viewModel::onImageDownloadTapped,
                onImageDeleteTapped = viewModel::onImageDeleteTapped,
                onImageMoveLeftTapped = viewModel::onImageMoveLeftTapped,
                onImageMoveRightTapped = viewModel::onImageMoveRightTapped,
                onAddImageViaCameraTapped = viewModel::onAddImageViaCameraTapped,
                onAddImageViaPickerTapped = viewModel::onAddImageViaPickerTapped,
                onDismissRequested = onDismissRequested,
                onImagesInfoButtonTapped = viewModel::onImagesInfoButtonTapped,
                onRunAIButtonTapped = viewModel::onRunAIButtonTapped,
                onVariantTemplatePicked = viewModel::onVariantTemplateSelected,
                onQuickPickStarToggled = viewModel::onQuickPickStarToggled,
                onRecordDetailsEditStarted = viewModel::onRecordDetailsEditStarted,
                onRecordDetailsEditCancelled = viewModel::onRecordDetailsEditCancelled,
            )

            is DialogHandle.ConfirmSwitchTemplateDialog -> ConfirmSwitchTemplateDialog(
                onConfirm = viewModel::onConfirmSwitchTemplateDespiteEdits,
                onDismiss = onDismissRequested,
            )

            is DialogHandle.EditTargetConfirmationDialog -> EditTargetConfirmationDialog(
                dialogHandle = dialogHandle,
                onEditTargetConfirmed = viewModel::onEditTargetConfirmed,
                onDismissRequested = onDismissRequested,
            )

            is DialogHandle.ViewImageDialog -> ImageDialog(
                dialogHandle = dialogHandle,
                onDismissRequested = onDismissRequested,
            )

            is DialogHandle.ImageInput -> {
                ImageInputView(
                    imageInput = dialogHandle,
                    viewModel = viewModel,
                    cacheFileStore = cacheFileStore,
                )
            }

            is DialogHandle.InfoDialog -> {
                InfoDialog(
                    message = dialogHandle.message,
                    onDismissRequested = onDismissRequested,
                )
            }

            null -> {
                // nothing to do
            }
        }
    }
}

@Composable
private fun ImageInputView(
    imageInput: DialogHandle.ImageInput,
    viewModel: ModalViewModel,
    cacheFileStore: FileStore,
) {
    when (imageInput.type) {
        is ImageInputType.Camera -> {
            val filename = "temp.$DefaultFoodPicExt"
            val launcher = rememberLauncherForActivityResult(
                contract = TakePicture(),
                onResult = { imageSaved ->
                    if (imageSaved) {
                        val uri = cacheFileStore.getOrCreateFile(filename).toUri()
                        viewModel.onImagesSelected(listOf(uri))
                    } else {
                        viewModel.onNoImageSelected()
                    }
                }
            )
            SideEffect {
                val uri = cacheFileStore.getOrCreateFile(filename).toUri()
                launcher.launch(uri)
            }
        }

        is ImageInputType.BrowseImages -> {
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickMultipleVisualMedia(),
                onResult = {
                    it.takeIf { it.isNotEmpty() }
                        ?.let { viewModel.onImagesSelected(it) }
                        ?: run { viewModel.onNoImageSelected() }
                }
            )
            SideEffect {
                val request =
                    PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                launcher.launch(request)
            }
        }
    }
}
