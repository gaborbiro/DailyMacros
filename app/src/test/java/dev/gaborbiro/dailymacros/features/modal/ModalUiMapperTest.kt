package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.shared.TemplateUiMapper
import dev.gaborbiro.dailymacros.repositories.records.domain.model.ComponentConfidence
import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.common.model.TopContributors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class ModalUiMapperTest {

    private val zone = ZoneId.of("UTC")
    private val mapper = ModalUiMapper(TemplateUiMapper())

    @Test
    fun `mapNutrientBreakdowns formats calories and notes`() {
        val record = Record(
            recordId = 1L,
            timestamp = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, zone),
            template = Template(
                dbId = 1L,
                imageFilenames = emptyList(),
                isRepresentativeOfMealByImageIndex = emptyList(),
                name = "Meal",
                description = "D",
                parentTemplateId = null,
                createdAtEpochMs = 0L,
                updatedAtEpochMs = 0L,
                isPending = false,
                nutrients = Nutrients(calories = 300, protein = 20f),
                notes = "Leftovers",
                mealComponents = emptyList(),
                topContributors = TopContributors(),
                quickPickOverride = null,
            ),
        )
        val ui = mapper.mapNutrientBreakdowns(record)
        assertNotNull(ui.calories)
        assertTrue(ui.calories!!.contains("300"))
        assertNotNull(ui.protein)
        assertEquals("Leftovers", ui.notes)
    }

    @Test
    fun `mapNutrientBreakdowns lists components and joins them into a summary with confidence markers`() {
        val record = recordWith(
            mealComponents = listOf(
                MealComponent(name = "bread", estimatedAmount = "1 slice", confidence = ComponentConfidence.HIGH),
                MealComponent(name = "marmalade", estimatedAmount = "~1 tbsp", confidence = ComponentConfidence.MEDIUM),
                MealComponent(name = "butter", estimatedAmount = "~5g", confidence = ComponentConfidence.LOW),
            ),
        )
        val ui = mapper.mapNutrientBreakdowns(record)
        assertEquals(
            listOf("- 1 slice bread", "- ~1 tbsp marmalade (?)", "- ~5g butter (??)"),
            ui.components,
        )
        assertEquals("1 slice bread · ~1 tbsp marmalade (?) · ~5g butter (??)", ui.componentsSummary)
    }

    @Test
    fun `mapNutrientBreakdowns has no components summary without components`() {
        val ui = mapper.mapNutrientBreakdowns(recordWith(mealComponents = emptyList()))
        assertNull(ui.componentsSummary)
    }

    private fun recordWith(mealComponents: List<MealComponent>) = Record(
        recordId = 1L,
        timestamp = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, zone),
        template = Template(
            dbId = 1L,
            imageFilenames = emptyList(),
            isRepresentativeOfMealByImageIndex = emptyList(),
            name = "Toast",
            description = "D",
            parentTemplateId = null,
            createdAtEpochMs = 0L,
            updatedAtEpochMs = 0L,
            isPending = false,
            nutrients = Nutrients(calories = 170),
            notes = "",
            mealComponents = mealComponents,
            topContributors = TopContributors(),
            quickPickOverride = null,
        ),
    )
}
