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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.gaborbiro.dailymacros.DefaultFoodPicExt
import dev.gaborbiro.dailymacros.data.db.AppDatabase
import dev.gaborbiro.dailymacros.data.file.FileStore
import dev.gaborbiro.dailymacros.data.file.FileStoreFactoryImpl
import dev.gaborbiro.dailymacros.data.image.ImageStoreImpl
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.features.common.DateUIMapper
import dev.gaborbiro.dailymacros.features.common.DeleteRecordUseCase
import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.common.view.ConfirmDeleteNutrientDataDialog
import dev.gaborbiro.dailymacros.features.common.view.ErrorDialog
import dev.gaborbiro.dailymacros.features.common.view.InfoDialog
import dev.gaborbiro.dailymacros.features.common.view.LocalImageStore
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.features.modal.model.ImagePickerState
import dev.gaborbiro.dailymacros.features.modal.model.ModalViewState
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodPicSummaryUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.views.EditTargetConfirmationDialog
import dev.gaborbiro.dailymacros.features.modal.views.ImageDialog
import dev.gaborbiro.dailymacros.features.modal.views.InputDialog
import dev.gaborbiro.dailymacros.features.modal.views.SelectRecordActionDialog
import dev.gaborbiro.dailymacros.features.modal.views.SelectTemplateActionDialog
import dev.gaborbiro.dailymacros.repo.chatgpt.AuthInterceptor
import dev.gaborbiro.dailymacros.repo.chatgpt.ChatGPTRepositoryImpl
import dev.gaborbiro.dailymacros.repo.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ContentEntryOutputContentDeserializer
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.OutputContentDeserializer
import dev.gaborbiro.dailymacros.repo.records.ApiMapper
import dev.gaborbiro.dailymacros.repo.records.RecordsRepositoryImpl
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

        fun launchRecordImageViewRequest(context: Context, recordId: Long) = launchActivity(
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
            CAMERA, PICK_IMAGE, TEXT_ONLY, VIEW_RECORD_DETAILS, VIEW_IMAGE, SELECT_RECORD_ACTION, SELECT_TEMPLATE_ACTION
        }

        private const val EXTRA_RECORD_ID = "record_id"
        private const val EXTRA_TEMPLATE_ID = "template_id"

        const val REQUEST_TIMEOUT_IN_SECONDS = 90L
    }

    private val fileStore = FileStoreFactoryImpl(this).getStore("public", keepFiles = true)
    private val cacheFileStore = FileStoreFactoryImpl(this).getStore("public", keepFiles = false)
    private val imageStore = ImageStoreImpl(fileStore)

    private val viewModel by lazy {
        val recordsRepository = RecordsRepositoryImpl(
            templatesDAO = AppDatabase.getInstance().templatesDAO(),
            recordsDAO = AppDatabase.getInstance().recordsDAO(),
            mapper = ApiMapper(),
            imageStore = imageStore,
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
        val dateUIMapper = DateUIMapper()
        val macrosUIMapper = MacrosUIMapper(dateUIMapper)
        val deleteRecordUseCase = DeleteRecordUseCase(recordsRepository)

        ModalViewModel(
            imageStore = imageStore,
            recordsRepository = recordsRepository,
            createRecordUseCase = CreateRecordUseCase(recordsRepository),
            editRecordUseCase = EditRecordUseCase(recordsRepository),
            editTemplateUseCase = EditTemplateUseCase(recordsRepository),
            validateEditRecordUseCase = ValidateEditRecordUseCase(recordsRepository, recordsMapper),
            validateCreateRecordUseCase = ValidateCreateRecordUseCase(),
            saveImageUseCase = SaveImageUseCase(this, imageStore),
            getRecordImageUseCase = GetRecordImageUseCase(recordsRepository, imageStore),
            getTemplateImageUseCase = GetTemplateImageUseCase(recordsRepository, imageStore),
            foodPicSummaryUseCase = FoodPicSummaryUseCase(imageStore, chatGPTRepository, recordsMapper),
            macrosUIMapper = macrosUIMapper,
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
            Action.CAMERA -> viewModel.onCreateRecordWithCameraDeeplink()
            Action.PICK_IMAGE -> viewModel.onCreateRecordWithImagePickerDeeplink()
            Action.TEXT_ONLY -> viewModel.onCreateRecordDeeplink()

            Action.VIEW_RECORD_DETAILS -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                viewModel.onViewRecordDetailsDeeplink(recordId)
            }

            Action.VIEW_IMAGE -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                if (recordId != -1L) {
                    viewModel.viewRecordImageDeeplink(recordId)
                } else {
                    val templateId = intent.getLongExtra(EXTRA_TEMPLATE_ID, -1L)
                    viewModel.onViewTemplateImageDeeplink(templateId)
                }
            }

            Action.SELECT_RECORD_ACTION -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                viewModel.onSelectRecordActionDeeplink(recordId)
            }

            Action.SELECT_TEMPLATE_ACTION -> {
                val templateId = intent.getLongExtra(EXTRA_TEMPLATE_ID, -1L)
                viewModel.onSelectTemplateActionDeeplink(templateId)
            }
        }

        setContent {
            val viewState: ModalViewState by viewModel.viewState.collectAsStateWithLifecycle()

            AppTheme {
                CompositionLocalProvider(LocalImageStore provides imageStore) {
                    viewState.dialogs.forEach {
                        Dialog(it)
                    }
                }
            }

            if (viewState.close) {
                finish()
            }
        }
    }

    @Composable
    fun Dialog(dialogState: DialogState?) {
        val onDismissRequested: () -> Unit =
            remember(dialogState) {
                {
                    dialogState
                        ?.let { viewModel.onDialogDismissRequested(it) }
                }
            }
        when (dialogState) {
            is DialogState.InputDialog -> InputDialog(
                dialogState = dialogState,
                onRecordDetailsSubmitRequested = viewModel::onRecordDetailsSubmitRequested,
                onRecordDetailsUserTyping = viewModel::onRecordDetailsUserTyping,
                onImageTapped = viewModel::onImageTapped,
                onAddImageViaCameraTapped = { viewModel.onAddImageViaCameraTapped(dialogState) },
                onAddImageViaPickerTapped = { viewModel.onAddImageViaPickerTapped(dialogState) },
                onDismissRequested = onDismissRequested,
                onImagesInfoButtonTapped = viewModel::onImagesInfoButtonTapped,
            )

            is DialogState.EditTargetConfirmationDialog -> EditTargetConfirmationDialog(
                dialogState = dialogState,
                onEditTargetConfirmed = viewModel::onEditTargetConfirmed,
                onDismissRequested = onDismissRequested,
            )

            is DialogState.ViewImageDialog -> ImageDialog(
                dialogState = dialogState,
                onDismissRequested = onDismissRequested,
            )

            is DialogState.SelectRecordActionDialog -> SelectRecordActionDialog(
                recordId = dialogState.recordId,
                onRepeatTapped = viewModel::onRepeatRecordTapped,
                onDetailsTapped = viewModel::onDetailsTapped,
                onDeleteTapped = viewModel::onDeleteTapped,
                onDismissRequested = onDismissRequested,
            )

            is DialogState.SelectTemplateActionDialog -> {
                SelectTemplateActionDialog(
                    templateId = dialogState.templateId,
                    onRepeatTapped = viewModel::onRepeatTemplateTapped,
                    onDismissRequested = onDismissRequested,
                )
            }

            is DialogState.ConfirmDeleteNutrientDataDialog -> {
                ConfirmDeleteNutrientDataDialog(
                    onConfirm = viewModel::onDeleteNutrientDataConfirmed,
                    onDismissRequested = onDismissRequested,
                )
            }

            is DialogState.NewImage -> {
                NewImageUI(
                    dialogState = dialogState,
                    viewModel = viewModel,
                    cacheFileStore = cacheFileStore,
                )
            }

            is DialogState.ErrorDialog -> {
                ErrorDialog(
                    errorMessage = dialogState.errorMessage,
                    onDismissRequested = onDismissRequested,
                )
            }

            is DialogState.InfoDialog -> {
                InfoDialog(
                    message = dialogState.message,
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
private fun NewImageUI(
    dialogState: DialogState.NewImage,
    viewModel: ModalViewModel,
    cacheFileStore: FileStore,
) {
    when (dialogState.imagePickerState) {
        is ImagePickerState.Take -> {
            val filename = "temp.$DefaultFoodPicExt"
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture(),
                onResult = { imageSaved ->
                    if (imageSaved) {
                        val uri = cacheFileStore.getOrCreateFile(filename).toUri()
                        viewModel.onImageSelected(uri, dialogState)
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

        is ImagePickerState.Select -> {
            val launcher = rememberLauncherForActivityResult(
                contract = PickVisualMedia(),
                onResult = {
                    it
                        ?.let { viewModel.onImageSelected(it, dialogState) }
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
