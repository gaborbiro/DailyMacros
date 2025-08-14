package dev.gaborbiro.dailymacros.features.modal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.gaborbiro.dailymacros.FoodPicExt
import dev.gaborbiro.dailymacros.data.chatgpt.AuthInterceptor
import dev.gaborbiro.dailymacros.data.chatgpt.ChatGPTRepositoryImpl
import dev.gaborbiro.dailymacros.data.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ContentEntryOutputContentDeserializer
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.OutputContentDeserializer
import dev.gaborbiro.dailymacros.data.records.DBMapper
import dev.gaborbiro.dailymacros.data.records.RecordsRepositoryImpl
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.features.common.DeleteRecordUseCase
import dev.gaborbiro.dailymacros.features.common.NutrientsUIMapper
import dev.gaborbiro.dailymacros.features.common.error.model.ErrorViewState
import dev.gaborbiro.dailymacros.features.common.error.views.ErrorDialog
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
import dev.gaborbiro.dailymacros.features.modal.usecase.GetTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.views.EditImageTargetConfirmationDialog
import dev.gaborbiro.dailymacros.features.modal.views.EditTargetConfirmationDialog
import dev.gaborbiro.dailymacros.features.modal.views.ImageDialog
import dev.gaborbiro.dailymacros.features.modal.views.InputDialog
import dev.gaborbiro.dailymacros.features.modal.views.SelectRecordActionDialog
import dev.gaborbiro.dailymacros.features.modal.views.SelectTemplateActionDialog
import dev.gaborbiro.dailymacros.features.widget.NotesWidget
import dev.gaborbiro.dailymacros.store.bitmap.BitmapStore
import dev.gaborbiro.dailymacros.store.db.AppDatabase
import dev.gaborbiro.dailymacros.store.file.FileStoreFactoryImpl
import okhttp3.OkHttpClient
import okhttp3.java.net.cookiejar.JavaNetCookieJar
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.util.concurrent.TimeUnit.SECONDS


class ModalActivity : AppCompatActivity() {

    companion object {

        fun launchAddRecordWithCamera(context: Context) =
            launchActivity(context, getCameraIntent(context))

        private fun getCameraIntent(context: Context) = getActionIntent(context, Action.CAMERA)

        fun launchAddRecordWithImagePicker(context: Context) =
            launchActivity(context, getImagePickerIntent(context))

        private fun getImagePickerIntent(context: Context) =
            getActionIntent(context, Action.PICK_IMAGE)

        fun launchViewRecordImage(context: Context, recordId: Long) = launchActivity(
            appContext = context,
            intent = getViewImageAction(context),
            EXTRA_RECORD_ID to recordId,
        )

        fun launchViewTemplateImage(context: Context, templateId: Long) = launchActivity(
            appContext = context,
            intent = getViewImageAction(context),
            EXTRA_TEMPLATE_ID to templateId,
        )

        private fun getViewImageAction(context: Context) =
            getActionIntent(context, Action.VIEW_IMAGE)

        fun launchAddRecord(context: Context) = launchActivity(context, getTextOnlyIntent(context))

        private fun getTextOnlyIntent(context: Context) = getActionIntent(context, Action.TEXT_ONLY)

        fun launchChangeImage(context: Context, recordId: Long) {
            launchActivity(
                appContext = context,
                intent = getActionIntent(context, Action.CHANGE_IMAGE),
                EXTRA_RECORD_ID to recordId
            )
        }

        fun launchViewRecordDetails(context: Context, recordId: Long) {
            launchActivity(
                appContext = context,
                intent = getActionIntent(context, Action.VIEW_RECORD_DETAILS),
                EXTRA_RECORD_ID to recordId
            )
        }

        fun launchSelectRecordAction(context: Context, recordId: Long) {
            launchActivity(
                appContext = context,
                intent = getActionIntent(context, Action.SELECT_RECORD_ACTION),
                EXTRA_RECORD_ID to recordId
            )
        }

        fun launchSelectTemplateAction(context: Context, templateId: Long) {
            launchActivity(
                appContext = context,
                intent = getActionIntent(context, Action.SELECT_TEMPLATE_ACTION),
                EXTRA_TEMPLATE_ID to templateId
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
                it.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }

        private const val EXTRA_ACTION = "extra_action"

        private enum class Action {
            CAMERA, PICK_IMAGE, TEXT_ONLY, CHANGE_IMAGE, VIEW_RECORD_DETAILS, VIEW_IMAGE, SELECT_RECORD_ACTION, SELECT_TEMPLATE_ACTION
        }

        private const val EXTRA_RECORD_ID = "record_id"
        private const val EXTRA_TEMPLATE_ID = "template_id"

        const val REQUEST_TIMEOUT_IN_SECONDS = 90L
    }

    private val fileStore = FileStoreFactoryImpl(this).getStore("public", keepFiles = true)
    private val cacheFileStore = FileStoreFactoryImpl(this).getStore("public", keepFiles = false)

    private val viewModel by lazy {
        val bitmapStore = BitmapStore(fileStore)
        val recordsRepository = RecordsRepositoryImpl(
            templatesDAO = AppDatabase.getInstance().templatesDAO(),
            recordsDAO = AppDatabase.getInstance().recordsDAO(),
            dBMapper = DBMapper(),
            bitmapStore = bitmapStore,
        )

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
        val nutrientsUIMapper = NutrientsUIMapper()
        val deleteRecordUseCase = DeleteRecordUseCase(recordsRepository)

        ModalScreenViewModel(
            bitmapStore = bitmapStore,
            recordsRepository = recordsRepository,
            fetchNutrientsUseCase = FetchNutrientsUseCase(
                this,
                bitmapStore,
                chatGPTRepository,
                recordsRepository,
                recordsMapper,
                nutrientsUIMapper,
            ),
            createRecordUseCase = CreateRecordUseCase(recordsRepository),
            editRecordUseCase = EditRecordUseCase(recordsRepository),
            editTemplateUseCase = EditTemplateUseCase(recordsRepository),
            validateEditRecordUseCase = ValidateEditRecordUseCase(recordsRepository),
            validateCreateRecordUseCase = ValidateCreateRecordUseCase(),
            saveImageUseCase = SaveImageUseCase(this, bitmapStore),
            validateEditImageUseCase = ValidateEditImageUseCase(recordsRepository),
            editRecordImageUseCase = EditRecordImageUseCase(recordsRepository, deleteRecordUseCase),
            editTemplateImageUseCase = EditTemplateImageUseCase(recordsRepository),
            getRecordImageUseCase = GetRecordImageUseCase(recordsRepository, bitmapStore),
            getTemplateImageUseCase = GetTemplateImageUseCase(recordsRepository, bitmapStore),
            foodPicSummaryUseCase = FoodPicSummaryUseCase(bitmapStore, chatGPTRepository, recordsMapper),
            nutrientsUIMapper = nutrientsUIMapper,
            deleteRecordUseCase = deleteRecordUseCase,
        )
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
            Action.CAMERA -> viewModel.addRecordWithCamera()
            Action.PICK_IMAGE -> viewModel.addRecordWithImagePicker()
            Action.TEXT_ONLY -> viewModel.addRecord()

            Action.CHANGE_IMAGE -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                viewModel.changeImage(recordId)
            }

            Action.VIEW_RECORD_DETAILS -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                viewModel.viewRecordDetails(recordId)
            }

            Action.VIEW_IMAGE -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                if (recordId != -1L) {
                    viewModel.viewRecordImage(recordId)
                } else {
                    val templateId = intent.getLongExtra(EXTRA_TEMPLATE_ID, -1L)
                    viewModel.viewTemplateImage(templateId)
                }
            }

            Action.SELECT_RECORD_ACTION -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                viewModel.selectRecordAction(recordId)
            }

            Action.SELECT_TEMPLATE_ACTION -> {
                val templateId = intent.getLongExtra(EXTRA_TEMPLATE_ID, -1L)
                viewModel.selectTemplateAction(templateId)
            }
        }

        setContent {
            val viewState: HostViewState by viewModel.viewState.collectAsStateWithLifecycle()

            DailyMacrosTheme {
                Dialog(viewState.dialog)
            }

            val error: State<ErrorViewState?> = viewModel.errorState.collectAsStateWithLifecycle()

            error.value?.let {
                DailyMacrosTheme {
                    ErrorDialog(error = it, onDismissRequested = viewModel::onErrorDialogDismissRequested)
                }
            }

            when (viewState.imagePicker) {
                ImagePickerState.Take -> {
                    val filename = "temp.$FoodPicExt"
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.TakePicture(),
                        onResult = {
                            val uri = cacheFileStore.getOrCreateFile(filename).toUri()
                            viewModel.onImageAvailable(uri)
                        }
                    )
                    SideEffect {
                        val uri = cacheFileStore.getOrCreateFile(filename).toUri()
                        launcher.launch(uri)
                    }
                }

                is ImagePickerState.Select, is ImagePickerState.ChangeImage -> {
                    val launcher = rememberLauncherForActivityResult(
                        contract = PickVisualMedia(),
                        onResult = {
                            viewModel.onImageAvailable(it)
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

            if (viewState.closeScreen) {
                finish()
            }
        }
    }

    @Composable
    fun Dialog(dialogState: DialogState?) {
        when (dialogState) {
            is DialogState.InputDialog -> InputDialog(
                dialogState = dialogState,
                onRecordDetailsSubmitRequested = viewModel::onRecordDetailsSubmitRequested,
                onRecordDetailsUserTyping = viewModel::onRecordDetailsUserTyping,
                onDismissRequested = viewModel::onDialogDismissRequested,
            )

            is DialogState.EditTargetConfirmationDialog -> EditTargetConfirmationDialog(
                dialogState = dialogState,
                onEditTargetConfirmed = viewModel::onEditTargetConfirmed,
                onDismissRequested = viewModel::onDialogDismissRequested,
            )

            is DialogState.EditImageTargetConfirmationDialog -> EditImageTargetConfirmationDialog(
                dialogState = dialogState,
                onEditImageTargetConfirmed = viewModel::onEditImageTargetConfirmed,
                onDismissRequested = viewModel::onDialogDismissRequested,
            )

            is DialogState.ViewImageDialog -> ImageDialog(
                dialogState = dialogState,
                onDismissRequested = viewModel::onDialogDismissRequested,
            )

            is DialogState.SelectRecordActionDialog -> SelectRecordActionDialog(
                recordId = dialogState.recordId,
                onRepeatTapped = viewModel::onRepeatRecordTapped,
                onEditTapped = viewModel::onEditTapped,
                onDeleteTapped = viewModel::onDeleteTapped,
                onDismissRequested = viewModel::onDialogDismissRequested,
            )

            is DialogState.SelectTemplateActionDialog -> {
                SelectTemplateActionDialog(
                    templateId = dialogState.templateId,
                    onRepeatTapped = viewModel::onRepeatTemplateTapped,
                    onDismissRequested = viewModel::onDialogDismissRequested,
                )
            }

            null -> {
                // nothing to do
            }
        }
    }
}
