package dev.gaborbiro.dailymacros.features.modal

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.modal.model.CloseSignal
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.usecase.ApplyQuickPickOverrideAndReloadWidgetUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.BaseRecordsRepositoryStub
import dev.gaborbiro.dailymacros.features.modal.usecase.BuildRecordDetailsViewDialogUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.LogMealFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.LogQuickPickMealUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ExportImageToGalleryUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodRecognitionUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.shared.ListMealVariantsForTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ModalRecordFixtures
import dev.gaborbiro.dailymacros.features.modal.usecase.ResolveFirstRecordIdForTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.UpdateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.features.shared.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.shared.ErrorUiMapper
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealReminderRefresher
import dev.gaborbiro.dailymacros.features.shared.TemplateUiMapper
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.common.model.TopContributors
import dev.gaborbiro.dailymacros.repositories.common.model.ZonedInstant
import dev.gaborbiro.dailymacros.repositories.common.model.toZonedDateTime
import dev.gaborbiro.dailymacros.repositories.common.model.toZonedInstant
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import dev.gaborbiro.dailymacros.test.WorkManagerTestSupport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowToast
import java.io.ByteArrayInputStream
import java.io.File
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class ModalViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val activeViewModels = mutableListOf<ModalViewModel>()

    @Before
    fun initWorkManager() {
        WorkManagerTestSupport.setUp(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        activeViewModels.forEach { it.viewModelScope.cancel() }
        activeViewModels.clear()
        WorkManagerTestSupport.tearDown(ApplicationProvider.getApplicationContext())
    }

    private class FakeImageStore : ImageStore {
        override suspend fun open(filename: String, thumbnail: Boolean) =
            ByteArrayInputStream(ByteArray(0))

        override suspend fun read(filename: String, thumbnail: Boolean) = null

        override suspend fun write(filename: String, bitmap: Bitmap) = Unit

        override suspend fun delete(filename: String) = Unit
    }

    private class VmFakeChatGpt : ChatGPTRepository {
        override suspend fun recogniseFood(request: FoodRecognitionRequest) = FoodRecognitionResult(title = "X", error = null)

        override suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysisResult =
            error("unused")

        override fun getDefaultFoodRecognitionPromptSegments() = emptyList<dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment>()
        override fun getDefaultNutrientAnalysisPromptSegments() = emptyList<dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment>()
        override fun getDefaultWeeklyInsightsPromptSegments() = emptyList<dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment>()
        override fun getDefaultOngoingWeekInsightsPromptSegments() = emptyList<dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment>()
        override suspend fun getWeeklyInsights(request: dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.WeeklyInsightsRequest): dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.WeeklyInsightsResult = error("unused")
        override suspend fun getOngoingInsights(request: dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.OngoingWeekInsightsRequest): dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.OngoingWeekInsightsResult = error("unused")
        override suspend fun validateApiKey(apiKey: String): Boolean = error("unused")
    }

    private val disabledTarget = Target(enabled = false)

    private var quickPickConfirmationEnabled = true

    private val testSettingsRepository = object : SettingsRepository {
        override fun getTargets(): Targets = Targets(
            calories = disabledTarget,
            protein = disabledTarget,
            salt = disabledTarget,
            fat = disabledTarget,
            carbs = disabledTarget,
            fibre = disabledTarget,
            ofWhichSaturated = disabledTarget,
            ofWhichSugar = disabledTarget,
        )

        override fun setTargets(targets: Targets) = Unit

        override fun getDiaryDayStartHour(): Int = 0

        override fun setDiaryDayStartHour(hourOfDay: Int) = Unit

        override fun getPromptCustomisations(): Map<String, String> = emptyMap()
        override fun setPromptCustomisations(values: Map<String, String>) = Unit
        override fun clearPromptCustomisations() = Unit
        override fun getPromptVersions(type: String) = emptyList<dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion>()
        override fun savePromptVersion(type: String, customisations: Map<String, String>) = dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion(1, 0L, emptyMap())
        override fun deletePromptVersion(version: Int) = Unit
        override fun getApiKeyOverride(): String? = null
        override fun setApiKeyOverride(key: String) = Unit
        override fun clearApiKeyOverride() = Unit
        override fun getQuickPickConfirmationEnabled() = quickPickConfirmationEnabled
    }

    private fun viewModel(
        repo: RecordsRepository = BaseRecordsRepositoryStub(),
        mealReminderRefresher: MealReminderRefresher = MealReminderRefresher { },
        applicationScope: CoroutineScope = CoroutineScope(testDispatcher),
    ): ModalViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val nutrients = TemplateUiMapper()
        val modalUiMapper = ModalUiMapper(nutrients)
        val buildDetails = BuildRecordDetailsViewDialogUseCase(modalUiMapper)
        val listMealVariants = ListMealVariantsForTemplateUseCase(
            recordsRepository = repo,
        )
        val createTemplate = CreateTemplateUseCase(repo)
        val createFromTemplate = CreateRecordFromTemplateUseCase(repo)
        val updateRec = UpdateRecordWithNewTemplateUseCase(repo, createTemplate)
        val imageStore = FakeImageStore()
        return ModalViewModel(
            application = app,
            modalUiMapper = modalUiMapper,
            imageStore = imageStore,
            recordsRepository = repo,
            buildRecordDetailsViewDialogUseCase = buildDetails,
            resolveFirstRecordIdForTemplateUseCase = ResolveFirstRecordIdForTemplateUseCase(repo),
            listMealVariantsForTemplateUseCase = listMealVariants,
            settingsRepository = testSettingsRepository,
            createRecordFromTemplateUseCase = createFromTemplate,
            createTemplateUseCase = createTemplate,
            createRecordWithNewTemplateUseCase = CreateRecordWithNewTemplateUseCase(createTemplate, createFromTemplate),
            updateRecordWithNewTemplateUseCase = updateRec,
            validateEditRecordUseCase = ValidateEditRecordUseCase(repo),
            validateCreateRecordUseCase = ValidateCreateRecordUseCase(),
            saveImageUseCase = SaveImageUseCase(app, imageStore, testSettingsRepository),
            exportImageToGalleryUseCase = ExportImageToGalleryUseCase(app, imageStore),
            getRecordImageUseCase = GetRecordImageUseCase(repo),
            getTemplateImageUseCase = GetTemplateImageUseCase(repo),
            foodRecognitionUseCase = FoodRecognitionUseCase(imageStore, VmFakeChatGpt(), testSettingsRepository),
            applyQuickPickOverrideAndReloadWidgetUseCase = ApplyQuickPickOverrideAndReloadWidgetUseCase(repo),
            logQuickPickMealUseCase = LogQuickPickMealUseCase(
                appContext = app,
                applicationScope = applicationScope,
                logMealFromTemplateUseCase = LogMealFromTemplateUseCase(app, repo, createFromTemplate),
                analyticsLogger = AnalyticsLogger(sinks = emptySet()),
            ),
            analyticsLogger = AnalyticsLogger(sinks = emptySet()),
            errorUiMapper = ErrorUiMapper(app),
            mealReminderRefresher = mealReminderRefresher,
        ).also { activeViewModels.add(it) }
    }

    @Test
    fun `text deeplink opens empty edit dialog`() {
        val vm = viewModel()
        vm.onCreateRecordWithTextDeeplinkReceived()
        val root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.Edit
        assertEquals("", root.title.text)
        assertTrue(root.imageFilenames.isEmpty())
        // Nothing to lose yet, so cancelling closes without asking.
        assertFalse(root.hasUnsavedEdits)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE) // Real ImageDecoder, which saving the shared photo uses.
    fun `photo shared into the app opens a new record that asks before discarding`() = runTest(testDispatcher) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val photo = File(app.cacheDir, "shared.png").apply {
            outputStream().use { Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val vm = viewModel()
        vm.onImagesShared(listOf(Uri.fromFile(photo)))
        // Saving the photo hops to Dispatchers.IO, outside the test scheduler, so wait in real time.
        val root = withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                vm.uiState.first { it.rootDialog is DialogHandle.RecordDetailsDialog.Edit }
            }
        }.rootDialog as DialogHandle.RecordDetailsDialog.Edit
        assertEquals(1, root.imageFilenames.size)
        // Same as picking the photo after opening the dialog from the widget.
        assertTrue(root.hasUnsavedEdits)
        // Stops the AI food recognition queued after the photo arrived; it isn't under test here.
        vm.viewModelScope.cancel()
    }

    @Test
    fun `photo recognition details open a new record that asks before discarding`() {
        val vm = viewModel()
        vm.onPhotoRecognitionDetailsDeeplinkReceived(recognisedTitle = "Pasta", imageFilename = "pic.jpg")
        val root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.Edit
        assertEquals("Pasta", root.title.text)
        assertEquals(listOf("pic.jpg"), root.imageFilenames)
        assertTrue(root.hasUnsavedEdits)
    }

    @Test
    fun `template details button opens details for first record of template`() = runTest(testDispatcher) {
        val tpl = ModalRecordFixtures.template(dbId = 99L, name = "Snack")
        val rec = ModalRecordFixtures.record(3L, tpl)
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun get(recordId: Long) = rec.takeIf { it.recordId == recordId }

            override suspend fun getRecordsByTemplate(templateId: Long) =
                if (templateId == 99L) listOf(rec) else emptyList()

            override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long): List<Long> =
                listOf(templateId)

            override suspend fun getTemplate(templateId: Long): Template =
                tpl.takeIf { it.dbId == templateId } ?: error("unexpected template $templateId")
        }
        val vm = viewModel(repo)
        vm.onTemplateDetailsButtonTapped(99L)
        advanceUntilIdle()
        val dlg = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertEquals("Snack", dlg.title.text)
        assertTrue(dlg.openedFromTemplateDetailsOnly)
    }

    @Test
    fun `title change updates root edit state`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.onCreateRecordWithTextDeeplinkReceived()
        vm.onTitleChanged(TextFieldValue("Hello"))
        advanceUntilIdle()
        val root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.Edit
        assertEquals("Hello", root.title.text)
    }

    @Test
    fun `image reorder swaps photos in edit mode`() = runTest(testDispatcher) {
        val tpl = ModalRecordFixtures.template(
            dbId = 7L,
            name = "Soup",
            imageFilenames = listOf("a.jpg", "b.jpg", "c.jpg"),
        )
        val rec = ModalRecordFixtures.record(5L, tpl)
        val repo = object : BaseRecordsRepositoryStub() {
            override fun observe(recordId: Long) = flowOf(rec)
            override suspend fun get(recordId: Long) = rec.takeIf { it.recordId == recordId }
            override suspend fun countRecordsForTemplate(templateId: Long) = 1
            override suspend fun getRecordsByTemplate(templateId: Long) = listOf(rec)
            override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = listOf(templateId)
            override suspend fun getTemplate(templateId: Long) = tpl
        }
        val vm = viewModel(repo)
        vm.onRecordDetailsButtonTapped(5L)
        advanceUntilIdle()
        vm.onRecordDetailsEditStarted()
        vm.onImageMoveRightTapped("a.jpg")
        advanceUntilIdle()
        var root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertEquals(listOf("b.jpg", "a.jpg", "c.jpg"), root.imageFilenames)
        vm.onImageMoveLeftTapped("c.jpg")
        advanceUntilIdle()
        root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertEquals(listOf("b.jpg", "c.jpg", "a.jpg"), root.imageFilenames)
        vm.onImageMoveLeftTapped("b.jpg")
        advanceUntilIdle()
        root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertEquals(listOf("b.jpg", "c.jpg", "a.jpg"), root.imageFilenames)
    }

    @Test
    fun `cancelling edit mode clears unsaved edits flag`() = runTest(testDispatcher) {
        val tpl = ModalRecordFixtures.template(
            dbId = 7L,
            name = "Soup",
            imageFilenames = listOf("a.jpg", "b.jpg"),
        )
        val rec = ModalRecordFixtures.record(5L, tpl)
        val repo = object : BaseRecordsRepositoryStub() {
            override fun observe(recordId: Long) = flowOf(rec)
            override suspend fun get(recordId: Long) = rec.takeIf { it.recordId == recordId }
            override suspend fun countRecordsForTemplate(templateId: Long) = 1
            override suspend fun getRecordsByTemplate(templateId: Long) = listOf(rec)
            override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = listOf(templateId)
            override suspend fun getTemplate(templateId: Long) = tpl
        }
        val vm = viewModel(repo)
        vm.onRecordDetailsButtonTapped(5L)
        advanceUntilIdle()
        vm.onRecordDetailsEditStarted()
        vm.onImageDeleteTapped("a.jpg")
        advanceUntilIdle()
        var root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertTrue(root.hasUnsavedEdits)
        vm.onRecordDetailsEditCancelled()
        root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertFalse(root.isEditing)
        assertEquals(listOf("a.jpg", "b.jpg"), root.imageFilenames)
        assertFalse(root.hasUnsavedEdits)
    }

    @Test
    fun `add and analyze submit saves template and record before clearing dialog`() = runTest(testDispatcher) {
        var saveTemplateCalls = 0
        var saveRecordCalls = 0
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun saveTemplate(templateToSave: TemplateToSave): Long {
                saveTemplateCalls++
                return 42L
            }

            override suspend fun saveRecord(templateId: Long, timestamp: ZonedInstant): Long {
                saveRecordCalls++
                assertEquals(42L, templateId)
                return 99L
            }
        }
        val vm = viewModel(repo)
        vm.onCreateRecordWithTextDeeplinkReceived()
        vm.onTitleChanged(TextFieldValue("Lunch"))
        vm.onSubmitButtonTapped()
        advanceUntilIdle()
        assertEquals(1, saveTemplateCalls)
        assertEquals(1, saveRecordCalls)
        assertNull(vm.uiState.value.rootDialog)
    }

    @Test
    fun `submit uses the timestamp captured when record creation started, not when it's saved`() =
        runTest(testDispatcher) {
            var savedAt: ZonedInstant? = null
            val repo = object : BaseRecordsRepositoryStub() {
                override suspend fun saveTemplate(templateToSave: TemplateToSave) = 42L
                override suspend fun saveRecord(templateId: Long, timestamp: ZonedInstant): Long {
                    savedAt = timestamp
                    return 99L
                }
            }
            val vm = viewModel(repo)
            vm.onCreateRecordWithTextDeeplinkReceived()
            val startedAt =
                (vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.Edit).startedAt.toZonedInstant()
            // Simulate the app being backgrounded for a while between opening the create-record
            // screen and finally submitting it.
            Thread.sleep(50)
            vm.onTitleChanged(TextFieldValue("Lunch"))
            vm.onSubmitButtonTapped()
            advanceUntilIdle()
            assertEquals(startedAt, savedAt)
        }

    @Test
    fun `editing timestamp persists the new value and updates the dialog state`() = runTest(testDispatcher) {
        var updatedRecord: Record? = null
        val tpl = ModalRecordFixtures.template(dbId = 7L, name = "Soup")
        val rec = ModalRecordFixtures.record(5L, tpl)
        val repo = object : BaseRecordsRepositoryStub() {
            override fun observe(recordId: Long) = flowOf(rec)
            override suspend fun get(recordId: Long) = rec.takeIf { it.recordId == recordId }
            override suspend fun countRecordsForTemplate(templateId: Long) = 1
            override suspend fun getRecordsByTemplate(templateId: Long) = listOf(rec)
            override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = listOf(templateId)
            override suspend fun getTemplate(templateId: Long) = tpl
            override suspend fun updateRecord(record: Record) {
                updatedRecord = record
            }
        }
        val refreshTriggers = mutableListOf<String>()
        val vm = viewModel(repo, mealReminderRefresher = { refreshTriggers += it })
        vm.onRecordDetailsButtonTapped(5L)
        advanceUntilIdle()
        val originalTimestamp =
            (vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View).timestamp
        vm.onEditTimestampTapped()
        advanceUntilIdle()
        val overlay = vm.uiState.value.overlayDialog as DialogHandle.EditTimestampDialog
        assertEquals(5L, overlay.recordId)
        assertEquals(originalTimestamp, overlay.timestamp)

        val newTimestamp = originalTimestamp.minusDays(1).withHour(8).withMinute(30)
        vm.onTimestampPicked(newTimestamp)
        advanceUntilIdle()

        assertEquals(newTimestamp, updatedRecord?.timestamp?.toZonedDateTime())
        assertEquals(5L, updatedRecord?.recordId)
        // Meal reminders re-plan straight away, since the meal may have moved to another slot.
        assertEquals(listOf("timestamp edit"), refreshTriggers)
        assertNull(vm.uiState.value.overlayDialog)
        assertEquals(
            newTimestamp,
            (vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View).timestamp,
        )
    }

    @Test
    fun `record details save with no edits closes without persisting`() = runTest(testDispatcher) {
        var updateRecordCalls = 0
        var updateTemplateCalls = 0
        val tpl = ModalRecordFixtures.template(dbId = 7L, name = "Soup")
        val rec = ModalRecordFixtures.record(5L, tpl)
        val repo = object : BaseRecordsRepositoryStub() {
            override fun observe(recordId: Long) = flowOf(rec)
            override suspend fun get(recordId: Long) = rec.takeIf { it.recordId == recordId }
            override suspend fun countRecordsForTemplate(templateId: Long) = 1
            override suspend fun getRecordsByTemplate(templateId: Long) = listOf(rec)
            override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = listOf(templateId)
            override suspend fun getTemplate(templateId: Long) = tpl
            override suspend fun updateRecord(record: Record) {
                updateRecordCalls++
            }

            override suspend fun updateTemplate(
                templateId: Long,
                name: String?,
                description: String?,
                templateImages: List<TemplateImageUpdate>?,
                nutrients: Pair<Nutrients, TopContributors>?,
                notes: String?,
                mealComponents: List<MealComponent>?,
            ) {
                updateTemplateCalls++
            }
        }
        val vm = viewModel(repo)
        vm.onRecordDetailsButtonTapped(5L)
        advanceUntilIdle()
        vm.onRecordDetailsEditStarted()
        vm.onSaveDetailsTapped()
        advanceUntilIdle()
        assertEquals(0, updateRecordCalls)
        assertEquals(0, updateTemplateCalls)
        assertNull(vm.uiState.value.rootDialog)
    }

    @Test
    fun `log meal again from template details creates single record`() = runTest(testDispatcher) {
        var saveRecordCalls = 0
        val tpl = ModalRecordFixtures.template(dbId = 99L, name = "Snack").copy(
            nutrients = Nutrients(calories = 200),
        )
        val rec = ModalRecordFixtures.record(3L, tpl)
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun get(recordId: Long) = rec.takeIf { it.recordId == recordId }
            override suspend fun getRecordsByTemplate(templateId: Long) =
                if (templateId == 99L) listOf(rec) else emptyList()
            override suspend fun countRecordsForTemplate(templateId: Long) = 1
            override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = listOf(templateId)
            override suspend fun getTemplate(templateId: Long) = tpl
            override suspend fun saveRecord(templateId: Long, timestamp: ZonedInstant): Long {
                saveRecordCalls++
                assertEquals(99L, templateId)
                return 100L
            }
        }
        val vm = viewModel(repo)
        vm.onTemplateDetailsButtonTapped(99L)
        advanceUntilIdle()
        vm.onSaveAndAddDetailsTapped()
        advanceUntilIdle()
        assertEquals(1, saveRecordCalls)
        assertNull(vm.uiState.value.rootDialog)
    }

    @Test
    fun `add again without edits only logs from existing template`() = runTest(testDispatcher) {
        var saveTemplateCalls = 0
        var saveRecordCalls = 0
        val tpl = ModalRecordFixtures.template(dbId = 7L, name = "Soup").copy(
            nutrients = Nutrients(calories = 100),
        )
        val rec = ModalRecordFixtures.record(5L, tpl)
        val repo = object : BaseRecordsRepositoryStub() {
            override fun observe(recordId: Long) = flowOf(rec)
            override suspend fun get(recordId: Long) = rec.takeIf { it.recordId == recordId }
            override suspend fun countRecordsForTemplate(templateId: Long) = 1
            override suspend fun getRecordsByTemplate(templateId: Long) = listOf(rec)
            override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = listOf(templateId)
            override suspend fun getTemplate(templateId: Long) = tpl
            override suspend fun saveTemplate(templateToSave: TemplateToSave): Long {
                saveTemplateCalls++
                return 99L
            }

            override suspend fun saveRecord(templateId: Long, timestamp: ZonedInstant): Long {
                saveRecordCalls++
                assertEquals(7L, templateId)
                return 88L
            }
        }
        val vm = viewModel(repo)
        vm.onRecordDetailsButtonTapped(5L)
        advanceUntilIdle()
        vm.onSaveAndAddDetailsTapped()
        advanceUntilIdle()
        assertEquals(0, saveTemplateCalls)
        assertEquals(1, saveRecordCalls)
        assertNull(vm.uiState.value.rootDialog)
    }

    @Test
    fun `add again without edits logs from template even when meal is unrecognised`() = runTest(testDispatcher) {
        var saveTemplateCalls = 0
        var saveRecordCalls = 0
        // Unrecognised meal: no title/macros yet. "Log meal again" must still duplicate it.
        val tpl = ModalRecordFixtures.template(dbId = 7L, name = "").copy(
            isPending = true,
        )
        val rec = ModalRecordFixtures.record(5L, tpl)
        val repo = object : BaseRecordsRepositoryStub() {
            override fun observe(recordId: Long) = flowOf(rec)
            override suspend fun get(recordId: Long) = rec.takeIf { it.recordId == recordId }
            override suspend fun countRecordsForTemplate(templateId: Long) = 1
            override suspend fun getRecordsByTemplate(templateId: Long) = listOf(rec)
            override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = listOf(templateId)
            override suspend fun getTemplate(templateId: Long) = tpl
            override suspend fun saveTemplate(templateToSave: TemplateToSave): Long {
                saveTemplateCalls++
                return 99L
            }

            override suspend fun saveRecord(templateId: Long, timestamp: ZonedInstant): Long {
                saveRecordCalls++
                assertEquals(7L, templateId)
                return 88L
            }
        }
        val vm = viewModel(repo)
        vm.onRecordDetailsButtonTapped(5L)
        advanceUntilIdle()
        vm.onSaveAndAddDetailsTapped()
        advanceUntilIdle()
        assertEquals(0, saveTemplateCalls)
        assertEquals(1, saveRecordCalls)
        assertNull(vm.uiState.value.rootDialog)
    }

    /** A quick pick's meal, already analysed (so logging it schedules no analysis), counting what gets logged. */
    private class QuickPickRepo : BaseRecordsRepositoryStub() {
        val loggedTemplateIds = mutableListOf<Long>()
        private val tpl = ModalRecordFixtures.template(dbId = 99L, name = "Snack").copy(
            nutrients = Nutrients(calories = 200),
        )

        override suspend fun getTemplate(templateId: Long) = tpl

        override suspend fun saveRecord(templateId: Long, timestamp: ZonedInstant): Long {
            loggedTemplateIds += templateId
            return 100L
        }
    }

    @Test
    fun `quick pick with confirmation off logs the meal and closes before returning`() = runTest(testDispatcher) {
        quickPickConfirmationEnabled = false
        val repo = QuickPickRepo()
        val vm = viewModel(repo)
        vm.onQuickPickWidgetConfirmDeeplinkReceived(99L, "Snack")
        // Closed synchronously, so ModalActivity can finish before its window is drawn.
        assertEquals(CloseSignal.CLOSE, vm.uiState.value.closeSignal)
        assertNull(vm.uiState.value.rootDialog)
        advanceUntilIdle()
        assertEquals(listOf(99L), repo.loggedTemplateIds)
        assertEquals("Meal logged", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `quick pick with confirmation on logs the meal only once log again is tapped`() = runTest(testDispatcher) {
        val repo = QuickPickRepo()
        val vm = viewModel(repo)
        vm.onQuickPickWidgetConfirmDeeplinkReceived(99L, "Snack")
        assertTrue(vm.uiState.value.rootDialog is DialogHandle.QuickPickWidgetConfirmDialog)
        assertNull(vm.uiState.value.closeSignal)
        assertTrue(repo.loggedTemplateIds.isEmpty())

        vm.onQuickPickWidgetLogAgainTapped()
        advanceUntilIdle()
        assertEquals(CloseSignal.CLOSE, vm.uiState.value.closeSignal)
        assertEquals(listOf(99L), repo.loggedTemplateIds)
        assertEquals("Meal logged", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `quick pick meal still gets logged after the screen's scope is cancelled`() = runTest(testDispatcher) {
        quickPickConfirmationEnabled = false
        val repo = QuickPickRepo()
        // Doesn't run launched work until the scheduler advances, unlike the screen's dispatcher.
        val applicationScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = viewModel(repo, applicationScope = applicationScope)
        vm.onQuickPickWidgetConfirmDeeplinkReceived(99L, "Snack")
        // The Activity finishes straight away, which clears the ViewModel.
        vm.viewModelScope.cancel()
        assertTrue(repo.loggedTemplateIds.isEmpty())
        advanceUntilIdle()
        assertEquals(listOf(99L), repo.loggedTemplateIds)
    }

    /** A single-variant meal whose records can be re-pointed at a new template (as re-analysis does). */
    private class ReanalysisRepo(
        initial: Record,
    ) : BaseRecordsRepositoryStub() {
        val records = MutableStateFlow(initial)
        val savedTemplates = mutableListOf<TemplateToSave>()

        override fun observe(recordId: Long) = records
        override suspend fun get(recordId: Long) = records.value.takeIf { it.recordId == recordId }
        override suspend fun countRecordsForTemplate(templateId: Long) = 1
        override suspend fun getRecordsByTemplate(templateId: Long) = listOf(records.value)
        override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = listOf(templateId)
        override suspend fun getTemplate(templateId: Long) = when (templateId) {
            records.value.template.dbId -> records.value.template
            42L -> ModalRecordFixtures.template(dbId = 42L, name = savedTemplates.last().name)
                .copy(description = savedTemplates.last().description)
            else -> error("unexpected template $templateId")
        }

        override suspend fun saveTemplate(templateToSave: TemplateToSave): Long {
            savedTemplates += templateToSave
            return 42L
        }

        override suspend fun updateRecord(record: Record) {
            records.value = record
        }
    }

    private fun analysedSoup() = ModalRecordFixtures.record(
        5L,
        ModalRecordFixtures.template(dbId = 7L, name = "Soup", description = "Lentil").copy(
            nutrients = Nutrients(calories = 300),
        ),
    )

    @Test
    fun `re-analyze adds the correction to the description and keeps the dialog open`() = runTest(testDispatcher) {
        val repo = ReanalysisRepo(analysedSoup())
        val vm = viewModel(repo)
        vm.onRecordDetailsButtonTapped(5L)
        advanceUntilIdle()
        var root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertTrue(root.allowCorrection)
        assertFalse(root.canReanalyze)

        vm.onCorrectionChanged(TextFieldValue("It was brown rice"))
        root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertTrue(root.canReanalyze)

        vm.onReanalyzeTapped()
        advanceUntilIdle()

        assertEquals(1, repo.savedTemplates.size)
        assertEquals("Soup", repo.savedTemplates.single().name)
        assertEquals("Lentil\nIt was brown rice", repo.savedTemplates.single().description)
        root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertEquals(42L, root.templateDbId)
        assertEquals("", root.correction.text)
        assertFalse(root.canReanalyze)
        assertTrue(root.showLoadingIndicator)
    }

    @Test
    fun `re-analysis shows analyzing until the worker has finished with the meal`() = runTest(testDispatcher) {
        val repo = ReanalysisRepo(analysedSoup())
        val vm = viewModel(repo)
        vm.onRecordDetailsButtonTapped(5L)
        advanceUntilIdle()
        vm.onCorrectionChanged(TextFieldValue("Small bowl"))
        vm.onReanalyzeTapped()
        advanceUntilIdle()

        fun showsAnalyzing() = (vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View).showLoadingIndicator

        // Forked template, not yet picked up by the worker: no results, not marked pending.
        assertTrue(showsAnalyzing())
        val forked = repo.records.value
        repo.records.value = forked.copy(template = forked.template.copy(isPending = true))
        advanceUntilIdle()
        assertTrue(showsAnalyzing())
        repo.records.value = forked.copy(
            template = forked.template.copy(isPending = false, nutrients = Nutrients(calories = 250)),
        )
        advanceUntilIdle()
        assertFalse(showsAnalyzing())
    }

    @Test
    fun `correction is capped so the description stays within its limit`() = runTest(testDispatcher) {
        val longDescription = "x".repeat(250)
        val repo = ReanalysisRepo(
            ModalRecordFixtures.record(5L, ModalRecordFixtures.template(dbId = 7L, description = longDescription)),
        )
        val vm = viewModel(repo)
        vm.onRecordDetailsButtonTapped(5L)
        advanceUntilIdle()
        vm.onCorrectionChanged(TextFieldValue("abcdefgh"))
        val root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertEquals("abcde", root.correction.text)
    }

    @Test
    fun `dictated title is appended to what's already typed`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.onCreateRecordWithTextDeeplinkReceived()
        vm.onTitleChanged(TextFieldValue("Porridge "))
        vm.onTitleSpoken("with blueberries")
        val root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.Edit
        assertEquals("Porridge with blueberries", root.title.text)
    }

    @Test
    fun `dictated description starts an empty field with a capital`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.onCreateRecordWithTextDeeplinkReceived()
        vm.onDescriptionSpoken(" two eggs on toast ")
        val root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.Edit
        assertEquals("Two eggs on toast", root.description.text)
    }

    @Test
    fun `dictated correction is capped like a typed one`() = runTest(testDispatcher) {
        val repo = ReanalysisRepo(
            ModalRecordFixtures.record(5L, ModalRecordFixtures.template(dbId = 7L, description = "x".repeat(250))),
        )
        val vm = viewModel(repo)
        vm.onRecordDetailsButtonTapped(5L)
        advanceUntilIdle()
        vm.onCorrectionSpoken("abcdefgh")
        val root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertEquals("Abcde", root.correction.text)
        assertTrue(root.canReanalyze)
    }

    @Test
    fun `quick pick details don't take corrections`() = runTest(testDispatcher) {
        val repo = ReanalysisRepo(analysedSoup())
        val vm = viewModel(repo)
        vm.onTemplateDetailsButtonTapped(7L)
        advanceUntilIdle()
        vm.onCorrectionChanged(TextFieldValue("Small bowl"))
        vm.onReanalyzeTapped()
        advanceUntilIdle()
        val root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertFalse(root.allowCorrection)
        assertEquals("", root.correction.text)
        assertTrue(repo.savedTemplates.isEmpty())
    }

    @Test
    fun `editing the title marks the update as one that re-analyzes, reordering photos doesn't`() = runTest(testDispatcher) {
        val tpl = ModalRecordFixtures.template(dbId = 7L, name = "Soup", imageFilenames = listOf("a.jpg", "b.jpg"))
        val repo = ReanalysisRepo(ModalRecordFixtures.record(5L, tpl))
        val vm = viewModel(repo)
        vm.onRecordDetailsButtonTapped(5L)
        advanceUntilIdle()
        vm.onRecordDetailsEditStarted()
        vm.onImageMoveRightTapped("a.jpg")
        var root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertTrue(root.hasUnsavedEdits)
        assertFalse(root.editsRequireAnalysis)
        vm.onTitleChanged(TextFieldValue("Stew"))
        root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertTrue(root.editsRequireAnalysis)
        vm.onRecordDetailsEditCancelled()
        root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertFalse(root.editsRequireAnalysis)
    }

    @Test
    fun `varied family save with edits updates shared template in place`() = runTest(testDispatcher) {
        var updateTemplateCalls = 0
        val tpl = ModalRecordFixtures.template(dbId = 7L, name = "Soup")
        val tplOther = ModalRecordFixtures.template(dbId = 8L, name = "Other")
        val rec = ModalRecordFixtures.record(5L, tpl)
        val repo = object : BaseRecordsRepositoryStub() {
            override fun observe(recordId: Long) = flowOf(rec)
            override suspend fun get(recordId: Long) = rec.takeIf { it.recordId == recordId }
            override suspend fun getRecordsByTemplate(templateId: Long) = when (templateId) {
                7L -> listOf(rec, rec.copy(recordId = 6L))
                8L -> listOf(ModalRecordFixtures.record(9L, tplOther))
                else -> emptyList()
            }

            override suspend fun countRecordsForTemplate(templateId: Long): Int =
                getRecordsByTemplate(templateId).size
            override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = listOf(7L, 8L)
            override suspend fun getTemplate(templateId: Long) = when (templateId) {
                7L -> tpl
                8L -> tplOther
                else -> error("unexpected $templateId")
            }
            override suspend fun updateTemplate(
                templateId: Long,
                name: String?,
                description: String?,
                templateImages: List<TemplateImageUpdate>?,
                nutrients: Pair<Nutrients, TopContributors>?,
                notes: String?,
                mealComponents: List<MealComponent>?,
            ) {
                updateTemplateCalls++
                assertEquals(7L, templateId)
                assertEquals("Stew", name)
            }
        }
        val vm = viewModel(repo)
        vm.onRecordDetailsButtonTapped(5L)
        advanceUntilIdle()
        vm.onRecordDetailsEditStarted()
        vm.onTitleChanged(TextFieldValue("Stew"))
        advanceUntilIdle()
        vm.onSaveDetailsTapped()
        advanceUntilIdle()
        assertEquals(1, updateTemplateCalls)
        assertNull(vm.uiState.value.rootDialog)
    }
}
