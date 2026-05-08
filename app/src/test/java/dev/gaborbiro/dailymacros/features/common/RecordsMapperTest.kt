package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.features.common.model.NutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientApiModel
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientsApiModel
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class RecordsMapperTest {

    private val zone = ZoneId.of("UTC")
    private val mapper = RecordsMapper()

    private fun stubTemplate(name: String, description: String) = Template(
        dbId = 1L,
        images = emptyList(),
        isRepresentativeOfMealByImageIndex = emptyList(),
        name = name,
        description = description,
        parentTemplateId = null,
        createdAtEpochMs = 0L,
        updatedAtEpochMs = 0L,
        isPending = false,
        nutrients = TemplateNutrientBreakdown(),
        notes = "",
        mealComponents = emptyList(),
        topContributors = TopContributors(),
        quickPickOverride = null,
    )

    private fun stubRecord(template: Template) = Record(
        recordId = 9L,
        timestamp = ZonedDateTime.of(2024, 6, 1, 10, 0, 0, 0, zone),
        template = template,
    )

    @Test
    fun `mapToNutrientAnalysisRequest copies title description and images`() {
        val t = stubTemplate(name = "Lunch", description = "Salad bowl")
        val req = mapper.mapToNutrientAnalysisRequest(stubRecord(t), listOf("aGVsbG8=", "d29ybGQ="))
        assertEquals(listOf("aGVsbG8=", "d29ybGQ="), req.base64Images)
        assertEquals("Lunch", req.title)
        assertEquals("Salad bowl", req.description)
    }

    @Test
    fun `mapNutrientAnalysisResponse passes through error when nutrients null`() {
        val res = NutrientAnalysisResult(
            nutrients = null,
            title = null,
            notes = null,
            components = emptyList(),
            isRepresentativeOfMealByImageIndex = emptyList(),
            cachedTokens = null,
            error = "Model failed",
        )
        val (pair, err) = mapper.mapNutrientAnalysisResponse(res)
        assertNull(pair)
        assertEquals("Model failed", err)
    }

    @Test
    fun `mapNutrientAnalysisResponse maps nutrients and contributors`() {
        val res = NutrientAnalysisResult(
            nutrients = NutrientsApiModel(
                calories = 400,
                protein = NutrientApiModel(20f, "chicken"),
                fat = NutrientApiModel(10f, "oil"),
                ofWhichSaturated = NutrientApiModel(2f, "butter"),
                carb = NutrientApiModel(40f, "rice"),
                ofWhichSugar = NutrientApiModel(5f, "honey"),
                ofWhichAddedSugar = NutrientApiModel(1f, "syrup"),
                salt = NutrientApiModel(1.5f, "soy"),
                fibre = NutrientApiModel(6f, "veg"),
            ),
            title = null,
            notes = null,
            components = emptyList(),
            isRepresentativeOfMealByImageIndex = emptyList(),
            cachedTokens = null,
            error = null,
        )
        val (pair, err) = mapper.mapNutrientAnalysisResponse(res)
        assertNull(err)
        val breakdown = pair!!.first
        val contributors = pair.second
        assertEquals(400, breakdown.calories)
        assertEquals(20f, breakdown.protein)
        assertEquals(10f, breakdown.fat)
        assertEquals(2f, breakdown.ofWhichSaturated)
        assertEquals(40f, breakdown.carbs)
        assertEquals(5f, breakdown.ofWhichSugar)
        assertEquals(1f, breakdown.ofWhichAddedSugar)
        assertEquals(1.5f, breakdown.salt)
        assertEquals(6f, breakdown.fibre)
        assertEquals("chicken", contributors.topProteinContributors)
        assertEquals("oil", contributors.topFatContributors)
        assertEquals("butter", contributors.topSaturatedFatContributors)
        assertEquals("rice", contributors.topCarbsContributors)
        assertEquals("honey", contributors.topSugarContributors)
        assertEquals("syrup", contributors.topAddedSugarContributors)
        assertEquals("soy", contributors.topSaltContributors)
        assertEquals("veg", contributors.topFibreContributors)
    }

    @Test
    fun `template nutrient breakdown round trips through ui breakdown`() {
        val template = TemplateNutrientBreakdown(
            calories = 100,
            protein = 11f,
            fat = 12f,
            ofWhichSaturated = 3f,
            carbs = 40f,
            ofWhichSugar = 5f,
            ofWhichAddedSugar = 1f,
            salt = 0.8f,
            fibre = 4f,
        )
        val ui: NutrientBreakdown = mapper.map(template)
        val back: TemplateNutrientBreakdown = mapper.map(ui)
        assertEquals(template, back)
    }

    @Test
    fun `mapNutrientAnalysisResponse maps partial nutrients leaving other contributors null`() {
        val nutrients = NutrientsApiModel(
            calories = null,
            protein = NutrientApiModel(null, "p"),
            fat = null,
            ofWhichSaturated = null,
            carb = null,
            ofWhichSugar = null,
            ofWhichAddedSugar = null,
            salt = null,
            fibre = null,
        )
        val res = NutrientAnalysisResult(
            nutrients = nutrients,
            title = null,
            notes = null,
            components = emptyList(),
            isRepresentativeOfMealByImageIndex = emptyList(),
            cachedTokens = null,
            error = null,
        )
        val (pair, _) = mapper.mapNutrientAnalysisResponse(res)
        val contributors = pair!!.second
        assertEquals("p", contributors.topProteinContributors)
        assertNull(contributors.topFatContributors)
    }
}
