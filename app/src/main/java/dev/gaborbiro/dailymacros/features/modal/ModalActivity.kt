package dev.gaborbiro.dailymacros.features.modal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.gaborbiro.dailymacros.data.chatgpt.AuthInterceptor
import dev.gaborbiro.dailymacros.data.chatgpt.ChatGPTRepositoryImpl
import dev.gaborbiro.dailymacros.data.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ContentEntryOutputContentDeserializer
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.OutputContentDeserializer
import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.design.NotesTheme
import dev.gaborbiro.dailymacros.features.common.BaseErrorDialogActivity
import dev.gaborbiro.dailymacros.features.common.ErrorViewModel
import dev.gaborbiro.dailymacros.features.common.RecordsMapper
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.features.modal.model.HostViewState
import dev.gaborbiro.dailymacros.features.modal.model.ImagePickerState
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.FetchNutrientsUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodPicSummaryUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ObserveMacrosUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.views.EditImageTargetConfirmationDialog
import dev.gaborbiro.dailymacros.features.modal.views.EditTargetConfirmationDialog
import dev.gaborbiro.dailymacros.features.modal.views.ImageDialog
import dev.gaborbiro.dailymacros.features.modal.views.InputDialog
import dev.gaborbiro.dailymacros.features.widget.NotesWidget
import dev.gaborbiro.dailymacros.generateImageFilename
import dev.gaborbiro.dailymacros.store.bitmap.BitmapStore
import dev.gaborbiro.dailymacros.store.file.FileStoreFactoryImpl
import okhttp3.OkHttpClient
import okhttp3.java.net.cookiejar.JavaNetCookieJar
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.util.concurrent.TimeUnit.SECONDS


class ModalActivity : BaseErrorDialogActivity() {

    companion object {

//        fun getDeleteRecordIntent(context: Context, recordId: Long): Intent {
//            return Intent(context, HostActivity::class.java).also {
//                it.putExtra(EXTRA_ACTION, ACTION_DELETE)
//                it.putExtra(EXTRA_RECORD_ID, recordId)
//                it.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
//            }
//        }

        fun launchCreateNoteWithCamera(context: Context) =
            launchActivity(context, getCameraIntent(context))

        private fun getCameraIntent(context: Context) = getActionIntent(context, Action.CAMERA)

        fun launchAddNoteWithImage(context: Context) =
            launchActivity(context, getImagePickerIntent(context))

        private fun getImagePickerIntent(context: Context) =
            getActionIntent(context, Action.PICK_IMAGE)

        fun launchShowImage(context: Context, recordId: Long) = launchActivity(
            appContext = context,
            intent = getShowImageIntent(context),
            EXTRA_RECORD_ID to recordId,
        )

        private fun getShowImageIntent(context: Context) =
            getActionIntent(context, Action.SHOW_IMAGE)

        fun launchAddNote(context: Context) = launchActivity(context, getTextOnlyIntent(context))

        private fun getTextOnlyIntent(context: Context) = getActionIntent(context, Action.TEXT_ONLY)

        fun launchRedoImage(context: Context, recordId: Long) {
            launchActivity(
                appContext = context,
                intent = getActionIntent(context, Action.REDO_IMAGE),
                EXTRA_RECORD_ID to recordId
            )
        }

        fun launchEdit(context: Context, recordId: Long) {
            launchActivity(
                appContext = context,
                intent = getActionIntent(context, Action.EDIT),
                EXTRA_RECORD_ID to recordId
            )
        }

        private fun launchActivity(
            appContext: Context,
            intent: Intent,
            vararg extras: Pair<String, Any>,
        ) {
            appContext.startActivity(intent.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtras(bundleOf(*extras))
            })
        }

        private fun getActionIntent(context: Context, action: Action) =
            Intent(context, ModalActivity::class.java).also {
                it.putExtra(EXTRA_ACTION, action.name)
            }

        private const val EXTRA_ACTION = "extra_action"

        private enum class Action {
            CAMERA, PICK_IMAGE, TEXT_ONLY, REDO_IMAGE, EDIT, SHOW_IMAGE
        }

        private const val EXTRA_RECORD_ID = "record_id"

        const val REQUEST_TIMEOUT_IN_SECONDS = 90L
    }

    private val fileStore = FileStoreFactoryImpl(this).getStore("public", keepFiles = true)

    private val viewModel by lazy {
        val recordsRepository = RecordsRepository.get(fileStore)
        val bitmapStore = BitmapStore(fileStore)

        val logger = HttpLoggingInterceptor().also {
            it.level = HttpLoggingInterceptor.Level.BODY
        }
        val authInterceptor = AuthInterceptor()
        val builder = OkHttpClient.Builder()
            .addNetworkInterceptor(logger)
            .addInterceptor(authInterceptor)
            .addNetworkInterceptor(authInterceptor)
            .callTimeout(REQUEST_TIMEOUT_IN_SECONDS, SECONDS)
            .connectTimeout(REQUEST_TIMEOUT_IN_SECONDS, SECONDS)
            .readTimeout(REQUEST_TIMEOUT_IN_SECONDS, SECONDS)
            .writeTimeout(REQUEST_TIMEOUT_IN_SECONDS, SECONDS)

        val okHttpClient = builder
            .cookieJar(JavaNetCookieJar(CookieManager()))
            .build()

        val gson = GsonBuilder()
            .registerTypeAdapter(OutputContent::class.java, OutputContentDeserializer())
            .registerTypeAdapter(
                object : TypeToken<ContentEntry<OutputContent>>() {}.type,
                ContentEntryOutputContentDeserializer()
            )
            .create()


        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val chatGPTRepository = ChatGPTRepositoryImpl(
            service = retrofit.create(ChatGPTService::class.java)
        )

        val recordsMapper = RecordsMapper()

        ModalScreenViewModel(
            bitmapStore = bitmapStore,
            recordsRepository = recordsRepository,
            fetchNutrientsUseCase = FetchNutrientsUseCase(this, bitmapStore, chatGPTRepository, recordsRepository, recordsMapper),
            createRecordUseCase = CreateRecordUseCase(recordsRepository),
            editRecordUseCase = EditRecordUseCase(recordsRepository),
            editTemplateUseCase = EditTemplateUseCase(recordsRepository),
            validateEditRecordUseCase = ValidateEditRecordUseCase(recordsRepository),
            validateCreateRecordUseCase = ValidateCreateRecordUseCase(),
            saveImageUseCase = SaveImageUseCase(this, bitmapStore),
            validateEditImageUseCase = ValidateEditImageUseCase(recordsRepository),
            editRecordImageUseCase = EditRecordImageUseCase(recordsRepository),
            editTemplateImageUseCase = EditTemplateImageUseCase(recordsRepository),
            getRecordImageUseCase = GetRecordImageUseCase(recordsRepository, bitmapStore),
            foodPicSummaryUseCase = FoodPicSummaryUseCase(bitmapStore, chatGPTRepository, recordsMapper),
            observeMacrosUseCase = ObserveMacrosUseCase(this, recordsRepository),
        )
    }

    override fun baseViewModel(): ErrorViewModel {
        return viewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent.getStringExtra(EXTRA_ACTION)
            ?.let { Action.valueOf(it) }
        intent.removeExtra(EXTRA_ACTION) // consume intent

        if (action == null) {
            finish()
            return
        }

        when (action) {
            Action.CAMERA -> viewModel.onStartWithCamera()
            Action.PICK_IMAGE -> viewModel.onStartWithImagePicker()
            Action.TEXT_ONLY -> viewModel.onStartWithJustText()

            Action.REDO_IMAGE -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                viewModel.onStartWithRedoImage(recordId)
            }

//            Action.DELETE -> {
//                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
//                viewModel.deleteRecord(recordId)
//                hideActionNotification()
//            }

            Action.EDIT -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                viewModel.onStartWithEdit(recordId)
            }

            Action.SHOW_IMAGE -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                viewModel.onStartWithShowImage(recordId)
            }
        }

        setContent {
            HandleErrors()
            val viewState: HostViewState by viewModel.viewState.collectAsStateWithLifecycle()

            if (viewState.showCamera) {
                val filename = generateImageFilename()

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture(),
                    onResult = {
                        viewModel.onPhotoTaken(filename)
                    }
                )
                SideEffect {
                    val uri = fileStore.createFile(filename).toUri()
                    launcher.launch(uri)
                }
            }

            when (viewState.imagePicker) {
                is ImagePickerState.Create, is ImagePickerState.EditImage -> {
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia(),
                        onResult = {
                            viewModel.onImagePicked(it)
                        }
                    )
                    SideEffect {
                        val request =
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        launcher.launch(request)
                    }
                }

                null -> {
                    // nothing to do
                }
            }

            if (viewState.refreshWidget) {
                NotesWidget.reload(this@ModalActivity)
            }

            NotesTheme {
                ModalView(viewState.dialog)
            }

            if (viewState.closeScreen) {
                finish()
            }
        }
    }

    @Composable
    fun ModalView(dialogState: DialogState?) {
        when (dialogState) {
            is DialogState.InputDialog -> InputDialog(
                dialogState = dialogState,
                onDialogDismissed = viewModel::onDialogDismissed,
                onRecordDetailsSubmitRequested = viewModel::onRecordDetailsSubmitRequested,
                onRecordDetailsUserTyping = viewModel::onRecordDetailsUserTyping,
            )

            is DialogState.EditTargetConfirmationDialog -> EditTargetConfirmationDialog(
                dialogState = dialogState,
                onEditTargetConfirmed = viewModel::onEditTargetConfirmed,
                onDialogDismissed = viewModel::onDialogDismissed,
            )

            is DialogState.EditImageTargetConfirmationDialog -> EditImageTargetConfirmationDialog(
                dialogState = dialogState,
                onDialogDismissed = viewModel::onDialogDismissed,
                onEditImageTargetConfirmed = viewModel::onEditImageTargetConfirmed,
            )

            is DialogState.ShowImageDialog -> ImageDialog(
                image = dialogState.bitmap,
                onDialogDismissed = viewModel::onDialogDismissed,
            )

            null -> {
                // no dialog is shown
            }
        }
    }
}
