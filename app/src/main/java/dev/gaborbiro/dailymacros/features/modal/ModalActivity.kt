package dev.gaborbiro.dailymacros.features.modal

import android.app.ComponentCaller
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.gaborbiro.dailymacros.AnalyticsLogger
import dev.gaborbiro.dailymacros.DefaultFoodPicExt
import dev.gaborbiro.dailymacros.data.db.AppDatabase
import dev.gaborbiro.dailymacros.data.file.FileStore
import dev.gaborbiro.dailymacros.data.file.FileStoreFactoryImpl
import dev.gaborbiro.dailymacros.data.image.ImageStoreImpl
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.features.common.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.common.DateUIMapper
import dev.gaborbiro.dailymacros.features.common.DeleteRecordUseCase
import dev.gaborbiro.dailymacros.features.common.NutrientsUIMapper
import dev.gaborbiro.dailymacros.features.common.RepeatRecordUseCase
import dev.gaborbiro.dailymacros.features.common.views.InfoDialog
import dev.gaborbiro.dailymacros.features.common.views.LocalImageStore
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.ImageInputType
import dev.gaborbiro.dailymacros.features.modal.model.ModalUIUpdates
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.EditTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodRecognitionUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.UpdateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.views.EditTargetConfirmationDialog
import dev.gaborbiro.dailymacros.features.modal.views.ImageDialog
import dev.gaborbiro.dailymacros.features.modal.views.RecordDetailsDialog
import dev.gaborbiro.dailymacros.features.modal.views.SelectRecordActionDialog
import dev.gaborbiro.dailymacros.features.modal.views.SelectTemplateActionDialog
import dev.gaborbiro.dailymacros.repo.chatgpt.AuthInterceptor
import dev.gaborbiro.dailymacros.repo.chatgpt.ChatGPTRepositoryImpl
import dev.gaborbiro.dailymacros.repo.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ContentEntryOutputContentDeserializer
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.OutputContentDeserializer
import dev.gaborbiro.dailymacros.repo.records.RecordsApiMapper
import dev.gaborbiro.dailymacros.repo.records.RecordsRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.java.net.cookiejar.JavaNetCookieJar
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.util.concurrent.TimeUnit.SECONDS


class ModalActivity : AppCompatActivity() {

    companion object {
        fun launchToAddRecordWithCamera(context: Context) =
            context.launchActivityInNewStack(Context::getCameraIntent)

        fun launchToAddRecordWithImagePicker(context: Context) =
            context.launchActivityInNewStack(Context::getImagePickerIntent)

        fun launchToShowRecordImage(context: Context, recordId: Long) =
            context.launchActivity { it.getShowRecordImageIntent(recordId) }

        fun launchToShowRecordImageNoApp(context: Context, recordId: Long) =
            context.launchActivityInNewStack { it.getShowRecordImageIntent(recordId) }

        fun launchToShowTemplateImage(context: Context, templateId: Long) =
            context.launchActivityInNewStack { it.getShowTemplateImageIntent(templateId) }

        fun launchToAddRecord(context: Context) =
            context.launchActivityInNewStack(Context::getTextOnlyIntent)

        fun launchViewRecordDetails(context: Context, recordId: Long) {
            context.launchActivity { it.getViewRecordDetailsIntent(recordId) }
        }

        fun launchToSelectRecordAction(context: Context, recordId: Long) {
            context.launchActivityInNewStack { it.getSelectRecordActionIntent(recordId) }
        }

        fun launchToSelectTemplateAction(context: Context, templateId: Long) {
            context.launchActivityInNewStack { it.getSelectTemplateActionIntent(templateId) }
        }

        const val REQUEST_TIMEOUT_IN_SECONDS = 90L
    }

    private val fileStore = FileStoreFactoryImpl(this).getStore("public", keepFiles = true)
    private val cacheFileStore = FileStoreFactoryImpl(this).getStore("public", keepFiles = false)
    private val imageStore = ImageStoreImpl(fileStore)

    private val viewModel by lazy {
        val analyticsLogger = AnalyticsLogger()
        val recordsRepository = RecordsRepositoryImpl(
            templatesDAO = AppDatabase.getInstance().templatesDAO(),
            recordsDAO = AppDatabase.getInstance().recordsDAO(),
            mapper = RecordsApiMapper(),
            imageStore = imageStore,
            analyticsLogger = analyticsLogger,
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
        val nutrientsUIMapper = NutrientsUIMapper(dateUIMapper)
        val deleteRecordUseCase = DeleteRecordUseCase(recordsRepository)
        val createRecordFromTemplateUseCase = CreateRecordFromTemplateUseCase(recordsRepository)
        val createTemplateUseCase = CreateTemplateUseCase(recordsRepository)
        val repeatRecordUseCase =
            RepeatRecordUseCase(recordsRepository, createRecordFromTemplateUseCase)

        ModalViewModel(
            imageStore = imageStore,
            recordsRepository = recordsRepository,
            createRecordFromTemplateUseCase = createRecordFromTemplateUseCase,
            createRecordWithNewTemplateUseCase = CreateRecordWithNewTemplateUseCase(createTemplateUseCase, createRecordFromTemplateUseCase),
            updateRecordWithNewTemplateUseCase = UpdateRecordWithNewTemplateUseCase(recordsRepository, createTemplateUseCase),
            editTemplateUseCase = EditTemplateUseCase(recordsRepository),
            repeatRecordUseCase = repeatRecordUseCase,
            validateEditRecordUseCase = ValidateEditRecordUseCase(recordsRepository),
            validateCreateRecordUseCase = ValidateCreateRecordUseCase(),
            saveImageUseCase = SaveImageUseCase(this, imageStore),
            getRecordImageUseCase = GetRecordImageUseCase(recordsRepository),
            getTemplateImageUseCase = GetTemplateImageUseCase(recordsRepository),
            foodRecognitionUseCase = FoodRecognitionUseCase(this, imageStore, chatGPTRepository, recordsMapper),
            nutrientsUIMapper = nutrientsUIMapper,
            deleteRecordUseCase = deleteRecordUseCase,
            analyticsLogger = analyticsLogger,
        )
    }

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
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()

            AppTheme {
                CompositionLocalProvider(LocalImageStore provides imageStore) {
                    val errorMessages = remember {
                        viewModel.uiUpdates
                            .filterIsInstance<ModalUIUpdates.Error>()
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

            if (viewState.close) {
                finish()
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
            Action.CAMERA -> viewModel.onCreateRecordWithCameraDeeplink()
            Action.BROWSE_IMAGES -> viewModel.onCreateRecordWithBrowseImagesDeeplink()
            Action.TEXT_ONLY -> viewModel.onCreateRecordWithTextDeeplink()

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
                onTitleChanged = viewModel::onTitleChanged,
                onDescriptionChanged = viewModel::onDescriptionChanged,
                onImageTapped = viewModel::onImageTapped,
                onImageDeleteTapped = viewModel::onImageDeleteTapped,
                onAddImageViaCameraTapped = viewModel::onAddImageViaCameraTapped,
                onAddImageViaPickerTapped = viewModel::onAddImageViaPickerTapped,
                onDismissRequested = onDismissRequested,
                onImagesInfoButtonTapped = viewModel::onImagesInfoButtonTapped,
                onRunAIButtonTapped = viewModel::onRunAIButtonTapped,
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

            is DialogHandle.SelectRecordActionDialog -> SelectRecordActionDialog(
                recordId = dialogHandle.recordId,
                title = dialogHandle.title,
                onRepeatTapped = viewModel::onRepeatRecordButtonTapped,
                onDetailsTapped = viewModel::onRecordDetailsButtonTapped,
                onAddToQuickPicksTapped = viewModel::onAddToQuickPicksTapped,
                onDeleteTapped = viewModel::onDeleteTapped,
                onDismissRequested = onDismissRequested,
            )

            is DialogHandle.SelectTemplateActionDialog -> {
                SelectTemplateActionDialog(
                    templateId = dialogHandle.templateId,
                    title = dialogHandle.title,
                    onRepeatButtonTapped = viewModel::onRepeatTemplateButtonTapped,
                    onDetailsButtonTapped = viewModel::onTemplateDetailsButtonTapped,
                    onRemoveFromQuickPicksTapped = viewModel::onRemoveFromQuickPicksTapped,
                    onDismissRequested = onDismissRequested,
                )
            }

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

//        ImageInputType.QuickPhoto -> {
//            val activity = LocalActivity.current!!
//            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
//                val filename = "temp.$DefaultFoodPicExt"
//                val uri = cacheFileStore.getOrCreateFile(filename).toUri()
//                val file = cacheFileStore.resolveFile(filename)
//                QuickCaptureScreen(file = file) {
//                    viewModel.onImageSelected(uri, imageInput)
//                }
//            } else {
//                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), 1001)
//                viewModel.onNoImageSelected()
//            }
//        }

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

//@Composable
//fun QuickCaptureScreen(
//    modifier: Modifier = Modifier,
//    file: File,
//    onImageCaptured: () -> Unit,
//) {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
//
//    Box(modifier = modifier.fillMaxSize()) {
//
//        AndroidView(
//            modifier = Modifier.fillMaxSize(),
//            factory = { ctx ->
//                val previewView = PreviewView(ctx).apply {
//                    layoutParams = ViewGroup.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.MATCH_PARENT
//                    )
//                    // force cropping to fill
//                    scaleType = PreviewView.ScaleType.FILL_CENTER
//                }
//
//                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
//                cameraProviderFuture.addListener({
//                    val cameraProvider = cameraProviderFuture.get()
//
//                    val metrics = ctx.resources.displayMetrics
//                    val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
//                    val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
//
//                    val preview = Preview.Builder()
//                        .setTargetResolution(screenSize)
//                        .setTargetRotation(rotation)
//                        .build()
//                        .also { it.surfaceProvider = previewView.surfaceProvider }
//
//                    imageCapture = ImageCapture.Builder()
//                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//                        .setTargetResolution(screenSize)
//                        .setTargetRotation(rotation)
//                        .build()
//
//                    try {
//                        cameraProvider.unbindAll()
//                        cameraProvider.bindToLifecycle(
//                            lifecycleOwner,
//                            CameraSelector.DEFAULT_BACK_CAMERA,
//                            preview,
//                            imageCapture
//                        )
//                    } catch (exc: Exception) {
//                        exc.printStackTrace()
//                    }
//                }, ContextCompat.getMainExecutor(ctx))
//
//                previewView
//            }
//        )
//
//        DisposableEffect(imageCapture) {
//            val listener = object : OrientationEventListener(context) {
//                override fun onOrientationChanged(orientation: Int) {
//                    val rotation = when (orientation) {
//                        in 45..134 -> Surface.ROTATION_270
//                        in 135..224 -> Surface.ROTATION_180
//                        in 225..314 -> Surface.ROTATION_90
//                        else -> Surface.ROTATION_0
//                    }
//                    imageCapture?.targetRotation = rotation
//                }
//            }
//            listener.enable()
//
//            onDispose { listener.disable() }
//        }
//
//        Button(
//            onClick = {
//                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
//                imageCapture?.takePicture(
//                    outputOptions,
//                    ContextCompat.getMainExecutor(context),
//                    object : ImageCapture.OnImageSavedCallback {
//                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                            onImageCaptured()
//                        }
//
//                        override fun onError(exc: ImageCaptureException) {
//                            exc.printStackTrace()
//                        }
//                    }
//                )
//            },
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(32.dp)
//                .size(80.dp),
//            shape = CircleShape,
//            contentPadding = PaddingValues(0.dp),
//        ) {
//            Text("●", fontSize = 32.sp)
//        }
//    }
//}
