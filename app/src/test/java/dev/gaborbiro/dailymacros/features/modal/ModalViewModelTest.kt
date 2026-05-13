package dev.gaborbiro.dailymacros.features.modal

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.gson.Gson
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.shared.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.shared.NutrientsUiMapper
import dev.gaborbiro.dailymacros.features.shared.RepeatRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.usecase.ApplyConfirmedSharedTemplateEditUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ApplyQuickPickOverrideAndReloadWidgetUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ApplyTemplateVariantPickerSelectionUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.BaseRecordsRepositoryStub
import dev.gaborbiro.dailymacros.features.modal.usecase.BuildRecordDetailsViewDialogUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.CreateTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.DeleteRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.FoodRecognitionUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetRecordImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetTemplateImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetVariabilityMatchForTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ModalRecordFixtures
import dev.gaborbiro.dailymacros.features.modal.usecase.OpenTemplateVariantPickerFromRecordDetailsUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ResolveFirstRecordIdForTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ResolveSelectRecordActionDialogUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ResolveSelectTemplateActionDialogUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.SaveImageUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.UpdateRecordWithNewTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateCreateRecordUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.ValidateEditRecordUseCase
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysis
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.VariabilityMiningResult
import dev.gaborbiro.dailymacros.repositories.records.TemplateVariabilityPreviewMapper
import dev.gaborbiro.dailymacros.repositories.records.VariabilityProfileMapper
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.VariabilityRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.MealVariabilityPersistedProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
@Config(sdk = [33], manifest = Config.NONE)
class ModalViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun initWorkManager() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            ApplicationProvider.getApplicationContext(),
        )
    }

    @After
    fun closeWorkManagerDb() {
        WorkManagerTestInitHelper.closeWorkDatabase()
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

        override suspend fun mineMealVariability(userMessageJson: String): VariabilityMiningResult =
            error("unused")
    }

    private fun variabilityRepoNoProfile(): VariabilityRepository = object : VariabilityRepository {
        override suspend fun getLatestProfile() = null
        override suspend fun replaceProfile(profile: MealVariabilityPersistedProfile) = Unit
        override suspend fun replaceProfileFromModelJson(
            profileJson: String,
            minedAtEpochMs: Long,
            templatesIngestWatermarkEpochMs: Long,
        ) = Unit
        override suspend fun clearProfile() = Unit
    }

    private fun viewModel(repo: RecordsRepository = BaseRecordsRepositoryStub()): ModalViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val nutrients = NutrientsUiMapper()
        val modalUiMapper = ModalUiMapper(nutrients)
        val previewMapper = TemplateVariabilityPreviewMapper()
        val getVariability = GetVariabilityMatchForTemplateUseCase(
            variabilityRepository = variabilityRepoNoProfile(),
            profileMapper = VariabilityProfileMapper(Gson()),
            previewMapper = previewMapper,
        )
        val buildDetails = BuildRecordDetailsViewDialogUseCase(getVariability, modalUiMapper)
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
            resolveSelectRecordActionDialogUseCase = ResolveSelectRecordActionDialogUseCase(repo),
            resolveSelectTemplateActionDialogUseCase = ResolveSelectTemplateActionDialogUseCase(repo),
            resolveFirstRecordIdForTemplateUseCase = ResolveFirstRecordIdForTemplateUseCase(repo),
            openTemplateVariantPickerFromRecordDetailsUseCase = OpenTemplateVariantPickerFromRecordDetailsUseCase(
                previewMapper,
            ),
            applyTemplateVariantPickerSelectionUseCase = ApplyTemplateVariantPickerSelectionUseCase(repo, previewMapper),
            createRecordFromTemplateUseCase = createFromTemplate,
            createRecordWithNewTemplateUseCase = CreateRecordWithNewTemplateUseCase(createTemplate, createFromTemplate),
            updateRecordWithNewTemplateUseCase = updateRec,
            repeatRecordUseCase = RepeatRecordUseCase(repo, createFromTemplate),
            validateEditRecordUseCase = ValidateEditRecordUseCase(repo),
            validateCreateRecordUseCase = ValidateCreateRecordUseCase(),
            saveImageUseCase = SaveImageUseCase(app, imageStore),
            getRecordImageUseCase = GetRecordImageUseCase(repo),
            getTemplateImageUseCase = GetTemplateImageUseCase(repo),
            foodRecognitionUseCase = FoodRecognitionUseCase(app, imageStore, VmFakeChatGpt()),
            deleteRecordUseCase = DeleteRecordUseCase(repo, app),
            applyQuickPickOverrideAndReloadWidgetUseCase = ApplyQuickPickOverrideAndReloadWidgetUseCase(repo),
            applyConfirmedSharedTemplateEditUseCase = ApplyConfirmedSharedTemplateEditUseCase(
                updateRecordWithNewTemplateUseCase = updateRec,
                recordsRepository = repo,
                appContext = app,
            ),
            analyticsLogger = AnalyticsLogger(),
        )
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
    fun `select record deeplink shows action dialog`() = runTest {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun get(recordId: Long) =
                ModalRecordFixtures.record(3L, ModalRecordFixtures.template(name = "Snack"))
        }
        val vm = viewModel(repo)
        vm.onSelectRecordActionDeeplinkReceived(3L)
        advanceUntilIdle()
        val dlg = vm.uiState.value.rootDialog as DialogHandle.SelectRecordActionDialog
        assertEquals(3L, dlg.recordId)
        assertEquals("Snack", dlg.title)
    }

    @Test
    fun `title change updates root edit state`() = runTest {
        val vm = viewModel()
        vm.onCreateRecordWithTextDeeplinkReceived()
        vm.onTitleChanged(TextFieldValue("Hello"))
        advanceUntilIdle()
        val root = vm.uiState.value.rootDialog as DialogHandle.RecordDetailsDialog.Edit
        assertEquals("Hello", root.title.text)
    }

    @Test
    fun `add and analyze submit saves template and record before clearing dialog`() = runTest {
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
}
