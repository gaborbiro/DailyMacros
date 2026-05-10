package dev.gaborbiro.dailymacros.features.modal.usecase

import com.google.gson.Gson
import dev.gaborbiro.dailymacros.features.common.NutrientsUiMapper
import dev.gaborbiro.dailymacros.features.modal.ModalUiMapper
import dev.gaborbiro.dailymacros.repositories.records.VariabilityProfileMapper
import dev.gaborbiro.dailymacros.repositories.records.domain.VariabilityRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.MealVariabilityPersistedProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildRecordDetailsViewDialogUseCaseTest {

    private fun variabilityRepoNeverCalled(): VariabilityRepository = object : VariabilityRepository {
        override suspend fun getLatestProfile() = error("unexpected")
        override suspend fun replaceProfile(profile: MealVariabilityPersistedProfile) = Unit
        override suspend fun replaceProfileFromModelJson(
            profileJson: String,
            minedAtEpochMs: Long,
            templatesIngestWatermarkEpochMs: Long,
        ) = Unit
        override suspend fun clearProfile() = Unit
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

    @Test
    fun `read only skips variability lookup`() = runBlocking {
        val gson = Gson()
        val getMatch = GetVariabilityMatchForTemplateUseCase(
            variabilityRepository = variabilityRepoNeverCalled(),
            profileMapper = VariabilityProfileMapper(gson),
        )
        val build = BuildRecordDetailsViewDialogUseCase(
            getVariabilityMatchForTemplateUseCase = getMatch,
            uiMapper = ModalUiMapper(NutrientsUiMapper()),
        )
        val record = ModalRecordFixtures.record(1L, ModalRecordFixtures.template(name = "Rice"))
        val dlg = build.execute(record, edit = false)
        assertFalse(dlg.allowEdit)
        assertNull(dlg.templateVariabilityPreview)
        assertTrue(dlg.variabilityArchetypes.isEmpty())
        assertTrue(dlg.title.text.contains("Rice"))
    }

    @Test
    fun `edit mode without profile yields empty variability ui`() = runBlocking {
        val gson = Gson()
        val getMatch = GetVariabilityMatchForTemplateUseCase(
            variabilityRepository = variabilityRepoNoProfile(),
            profileMapper = VariabilityProfileMapper(gson),
        )
        val build = BuildRecordDetailsViewDialogUseCase(
            getVariabilityMatchForTemplateUseCase = getMatch,
            uiMapper = ModalUiMapper(NutrientsUiMapper()),
        )
        val record = ModalRecordFixtures.record(1L, ModalRecordFixtures.template())
        val dlg = build.execute(record, edit = true)
        assertTrue(dlg.allowEdit)
        assertNull(dlg.templateVariabilityPreview)
        assertTrue(dlg.variabilityArchetypes.isEmpty())
        assertTrue(dlg.variabilityArchetypePickerEntries.isEmpty())
    }
}
