package dev.gaborbiro.dailymacros.features.modal

import androidx.compose.ui.text.input.TextFieldValue
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot
import dev.gaborbiro.dailymacros.features.shared.TemplateUiMapper
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.repositories.records.domain.model.ComponentConfidence
import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.common.model.TopContributors
import dev.gaborbiro.dailymacros.repositories.common.model.toZonedInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            timestamp = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, zone).toZonedInstant(),
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
        assertEquals(listOf("Calories", "Protein"), ui.nutrients.map { it.label })
        assertTrue(ui.nutrients[0].amount.contains("300"))
        assertEquals("Leftovers", ui.notes)
    }

    @Test
    fun `mapNutrientBreakdowns nests of-which rows and keeps sources apart from amounts`() {
        val record = recordWith(mealComponents = emptyList()).let { r ->
            r.copy(
                template = r.template.copy(
                    nutrients = Nutrients(fat = 10f, ofWhichSaturated = 4f, carbs = 30f, ofWhichSugar = 12f, ofWhichAddedSugar = 5f),
                    topContributors = TopContributors(topFatContributors = "butter"),
                ),
            )
        }
        val rows = mapper.mapNutrientBreakdowns(record).nutrients
        assertEquals(listOf("Fat", "Saturated", "Carbs", "Sugar", "Added sugar"), rows.map { it.label })
        assertEquals(listOf(0, 1, 0, 1, 2), rows.map { it.indent })
        assertEquals("butter", rows[0].sources)
        assertTrue(rows.none { it.amount.contains("(") || it.label.contains(":") })
    }

    @Test
    fun `mapNutrientBreakdowns lists components and joins them into a summary with plain-word confidence for uncertain ones`() {
        val record = recordWith(
            mealComponents = listOf(
                MealComponent(name = "bread", estimatedAmount = "1 slice", confidence = ComponentConfidence.HIGH),
                MealComponent(name = "marmalade", estimatedAmount = "~1 tbsp", confidence = ComponentConfidence.MEDIUM),
                MealComponent(name = "butter", estimatedAmount = "~5g", confidence = ComponentConfidence.LOW),
            ),
        )
        val ui = mapper.mapNutrientBreakdowns(record)
        assertEquals(
            listOf("- 1 slice bread", "- ~1 tbsp marmalade (likely)", "- ~5g butter (guess)"),
            ui.components,
        )
        assertEquals("1 slice bread · ~1 tbsp marmalade (likely) · ~5g butter (guess)", ui.componentsSummary)
    }

    @Test
    fun `mapNutrientBreakdowns has no components summary without components`() {
        val ui = mapper.mapNutrientBreakdowns(recordWith(mealComponents = emptyList()))
        assertNull(ui.componentsSummary)
    }

    @Test
    fun `appendCorrection puts the correction on its own line after the description`() {
        assertEquals("Leftover rice\nIt was brown rice", mapper.appendCorrection("Leftover rice ", " It was brown rice"))
    }

    @Test
    fun `appendCorrection without a description is just the correction`() {
        assertEquals("About 200 g", mapper.appendCorrection("  ", "About 200 g"))
    }

    @Test
    fun `correctionCapacity leaves room for the separator`() {
        assertEquals(256 - 5 - 1, mapper.correctionCapacity("Curry", maxDescriptionLength = 256))
        assertEquals(256, mapper.correctionCapacity("", maxDescriptionLength = 256))
        assertEquals(0, mapper.correctionCapacity("x".repeat(300), maxDescriptionLength = 256))
    }

    @Test
    fun `editsRequireMacroAnalysis ignores reordered photos and whitespace but not new wording or photos`() {
        val pristine = viewDialog(title = "Soup", description = "Lentil", images = listOf("a", "b"))
        assertFalse(mapper.editsRequireMacroAnalysis(pristine))
        assertFalse(mapper.editsRequireMacroAnalysis(pristine.copy(imageFilenames = listOf("b", "a"))))
        assertFalse(mapper.editsRequireMacroAnalysis(pristine.copy(title = TextFieldValue("Soup "))))
        assertTrue(mapper.editsRequireMacroAnalysis(pristine.copy(title = TextFieldValue("Stew"))))
        assertTrue(mapper.editsRequireMacroAnalysis(pristine.copy(description = TextFieldValue("Lentil, no cream"))))
        assertTrue(mapper.editsRequireMacroAnalysis(pristine.copy(imageFilenames = listOf("a", "b", "c"))))
    }

    private fun viewDialog(title: String, description: String, images: List<String>) =
        DialogHandle.RecordDetailsDialog.View(
            recordId = 1L,
            templateDbId = 1L,
            variabilityAnchorTemplateDbId = 1L,
            nutrientBreakdown = null,
            compactNutrients = NutrientsUiModel(
                calories = null,
                protein = null,
                fat = null,
                carbs = null,
                salt = null,
                fibre = null,
            ),
            allowEdit = true,
            isEditing = true,
            titleHint = "",
            title = TextFieldValue(title),
            description = TextFieldValue(description),
            imageFilenames = images,
            pristineSnapshot = RecordDetailsPristineSnapshot(
                templateDbId = 1L,
                title = title,
                description = description,
                imageFilenames = images,
            ),
            timestamp = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, zone),
        )

    private fun recordWith(mealComponents: List<MealComponent>) = Record(
        recordId = 1L,
        timestamp = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, zone).toZonedInstant(),
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
