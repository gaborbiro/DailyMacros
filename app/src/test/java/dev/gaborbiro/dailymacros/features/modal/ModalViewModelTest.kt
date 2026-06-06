package dev.gaborbiro.dailymacros.features.modal

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import dev.gaborbiro.dailymacros.test.WorkManagerTestSupport
import kotlinx.coroutines.cancel
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.shared.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.shared.NutrientsUiMapper
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.hasUnsavedEdits
import dev.gaborbiro.dailymacros.features.modal.model.imagesRequireMacroReanalysis
import dev.gaborbiro.dailymacros.features.modal.usecase.ApplyConfirmedSharedTemplateEditUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ApplyQuickPickOverrideAndReloadWidgetUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.BaseRecordsRepositoryStub
import dev.gaborbiro.dailymacros.features.modal.usecase.BuildRecordDetailsViewDialogUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodRecognitionUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ListMealVariantsForTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ModalRecordFixtures
import dev.gaborbiro.dailymacros.features.modal.usecase.ResolveFirstRecordIdForTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.UpdateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysis
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
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
        override suspend fun recogniseFood(request: FoodRecognitionRequest) = FoodRecognitionResult(
            title = "X",
            description = "Y",
            cachedTokens = 0,
        )

        override suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysis =
            error("unused")
    }

    private val disabledTarget = Target(enabled = false)

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
    }

    private fun viewModel(repo: RecordsRepository = BaseRecordsRepositoryStub()): ModalViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val nutrients = NutrientsUiMapper()
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
            saveImageUseCase = SaveImageUseCase(app, imageStore),
            getRecordImageUseCase = GetRecordImageUseCase(repo),
            getTemplateImageUseCase = GetTemplateImageUseCase(repo),
            foodRecognitionUseCase = FoodRecognitionUseCase(app, imageStore, VmFakeChatGpt()),
            applyQuickPickOverrideAndReloadWidgetUseCase = ApplyQuickPickOverrideAndReloadWidgetUseCase(repo),
            applyConfirmedSharedTemplateEditUseCase = ApplyConfirmedSharedTemplateEditUseCase(
                updateRecordWithNewTemplateUseCase = updateRec,
                recordsRepository = repo,
                appContext = app,
            ),
            analyticsLogger = AnalyticsLogger(),
        ).also { activeViewModels.add(it) }
    }

    @Test
    fun `text deeplink opens empty edit dialog`() {
        val vm = viewModel()
        vm.onCreateRecordWithTextDeeplinkReceived()
        val root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.Edit
        assertEquals("", root.title.text)
        assertTrue(root.images.isEmpty())
    }

    @Test
    fun `view template details deeplink opens details for first record of template`() = runTest(testDispatcher) {
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
        vm.onViewTemplateDetailsDeeplinkReceived(99L)
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
            images = listOf("a.jpg", "b.jpg", "c.jpg"),
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
        assertEquals(listOf("b.jpg", "a.jpg", "c.jpg"), root.images)
        vm.onImageMoveLeftTapped("c.jpg")
        advanceUntilIdle()
        root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertEquals(listOf("b.jpg", "c.jpg", "a.jpg"), root.images)
        vm.onImageMoveLeftTapped("b.jpg")
        advanceUntilIdle()
        root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.View
        assertEquals(listOf("b.jpg", "c.jpg", "a.jpg"), root.images)
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

            override suspend fun saveRecord(templateId: Long, timestamp: ZonedDateTime): Long {
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
                nutrients: Pair<TemplateNutrientBreakdown, TopContributors>?,
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
            nutrients = TemplateNutrientBreakdown(calories = 200),
        )
        val rec = ModalRecordFixtures.record(3L, tpl)
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun get(recordId: Long) = rec.takeIf { it.recordId == recordId }
            override suspend fun getRecordsByTemplate(templateId: Long) =
                if (templateId == 99L) listOf(rec) else emptyList()
            override suspend fun countRecordsForTemplate(templateId: Long) = 1
            override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = listOf(templateId)
            override suspend fun getTemplate(templateId: Long) = tpl
            override suspend fun saveRecord(templateId: Long, timestamp: ZonedDateTime): Long {
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
            nutrients = TemplateNutrientBreakdown(calories = 100),
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

            override suspend fun saveRecord(templateId: Long, timestamp: ZonedDateTime): Long {
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
                nutrients: Pair<TemplateNutrientBreakdown, TopContributors>?,
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
