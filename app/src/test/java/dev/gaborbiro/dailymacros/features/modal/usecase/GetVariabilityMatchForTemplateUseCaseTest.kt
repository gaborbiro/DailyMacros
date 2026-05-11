package dev.gaborbiro.dailymacros.features.modal.usecase

import com.google.gson.Gson
import dev.gaborbiro.dailymacros.repositories.records.TemplateVariabilityPreviewMapper
import dev.gaborbiro.dailymacros.repositories.records.VariabilityProfileMapper
import dev.gaborbiro.dailymacros.repositories.records.domain.VariabilityRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.MealVariabilityProfileSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetVariabilityMatchForTemplateUseCaseTest {

    private val gson = Gson()
    private val profileMapper = VariabilityProfileMapper(gson)

    private val profileJsonOneArchetype = """
        {
          "archetypes": [
            {
              "archetype_id": "meal_a",
              "display_name": "Meal A",
              "title_aliases": [],
              "evidence_count": 1,
              "deprecated": false,
              "slots": [
                {
                  "slot_id": "spread",
                  "role_display_name": "Spread",
                  "nutritional_levers": [],
                  "is_high_variability": false,
                  "confidence": 0.5,
                  "rationale": "",
                  "variants": [
                    {
                      "variant_id": "butter",
                      "variant_label": "Butter",
                      "supporting_entry_evidence": [
                        { "logged_at": "t1", "template_id": 7 }
                      ]
                    },
                    {
                      "variant_id": "jam",
                      "variant_label": "Jam",
                      "supporting_entry_evidence": [
                        { "logged_at": "t2", "template_id": 99 }
                      ]
                    }
                  ]
                }
              ]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `throws when no profile snapshot`() = runBlocking {
        val repo = object : VariabilityRepository {
            override suspend fun getLatestProfile() = null
            override suspend fun replaceProfile(profile: dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.MealVariabilityPersistedProfile) =
                Unit
            override suspend fun replaceProfileFromModelJson(
                profileJson: String,
                minedAtEpochMs: Long,
                templatesIngestWatermarkEpochMs: Long,
            ) = Unit
            override suspend fun clearProfile() = Unit
        }
        val err = runCatching {
            GetVariabilityMatchForTemplateUseCase(repo, profileMapper, TemplateVariabilityPreviewMapper()).execute(7L)
        }.exceptionOrNull()
        assertTrue(err is NoVariabilityProfileLoadedException)
    }

    @Test
    fun `no slots for template returns empty picker entries but keeps parsed archetypes`() = runBlocking {
        val snap = MealVariabilityProfileSnapshot(
            minedAtEpochMs = 1L,
            profileJson = profileJsonOneArchetype,
            templatesIngestWatermarkEpochMs = 42L,
        )
        val repo = object : VariabilityRepository {
            override suspend fun getLatestProfile() = snap
            override suspend fun replaceProfile(profile: dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.MealVariabilityPersistedProfile) =
                Unit
            override suspend fun replaceProfileFromModelJson(
                profileJson: String,
                minedAtEpochMs: Long,
                templatesIngestWatermarkEpochMs: Long,
            ) = Unit
            override suspend fun clearProfile() = Unit
        }
        val match = GetVariabilityMatchForTemplateUseCase(repo, profileMapper, TemplateVariabilityPreviewMapper()).execute(999L)
        assertTrue(match.preview.slots.isEmpty())
        assertTrue(match.archetypePickerEntries.isEmpty())
        assertEquals("meal_a", match.variabilityArchetypes.single().archetypeKey)
    }

    @Test
    fun `template with slots yields picker entries`() = runBlocking {
        val snap = MealVariabilityProfileSnapshot(
            minedAtEpochMs = 1L,
            profileJson = profileJsonOneArchetype,
            templatesIngestWatermarkEpochMs = 0L,
        )
        val repo = object : VariabilityRepository {
            override suspend fun getLatestProfile() = snap
            override suspend fun replaceProfile(profile: dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.MealVariabilityPersistedProfile) =
                Unit
            override suspend fun replaceProfileFromModelJson(
                profileJson: String,
                minedAtEpochMs: Long,
                templatesIngestWatermarkEpochMs: Long,
            ) = Unit
            override suspend fun clearProfile() = Unit
        }
        val match = GetVariabilityMatchForTemplateUseCase(repo, profileMapper, TemplateVariabilityPreviewMapper()).execute(7L)
        assertEquals(1, match.preview.slots.size)
        assertEquals(1, match.archetypePickerEntries.size)
        assertEquals("meal_a", match.archetypePickerEntries.single().archetypeKey)
        assertEquals(1, match.archetypePickerEntries.single().slots.size)
    }
}
