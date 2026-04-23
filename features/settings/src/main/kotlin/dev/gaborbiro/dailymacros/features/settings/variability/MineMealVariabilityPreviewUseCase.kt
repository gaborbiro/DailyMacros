package dev.gaborbiro.dailymacros.features.settings.variability

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.ComponentConfidence
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record

private const val MAX_RECORDS = 250
private const val MAX_NOTES_CHARS = 1200
private const val MAX_DESCRIPTION_CHARS = 1200

data class VariabilityMiningPreview(
    /** Pretty-printed JSON sent to the model (for inspection). */
    val requestJsonPretty: String,
    /** Pretty-printed JSON returned by the model. */
    val responseJsonPretty: String,
)

class MineMealVariabilityPreviewUseCase(
    private val recordsRepository: RecordsRepository,
    private val chatGPTRepository: ChatGPTRepository,
) {

    private val compactGson: Gson = GsonBuilder().create()
    private val prettyGson: Gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

    suspend fun execute(): VariabilityMiningPreview {
        val records = recordsRepository.getRecentRecords(MAX_RECORDS)
        val envelope = VariabilityMiningUserEnvelope(
            schema_version = "1.0",
            merge_mode = "incremental",
            existing_profile = null,
            constraints = VariabilityConstraints(
                max_archetypes = 50,
                min_evidence_for_high_variability_slot = 2,
                min_variants_per_slot = 2,
                max_notes_chars_per_entry = MAX_NOTES_CHARS,
                max_description_chars_per_entry = MAX_DESCRIPTION_CHARS,
            ),
            meal_observations = records.map { it.toObservation() },
        )
        val userJson = compactGson.toJson(envelope)
        val result = chatGPTRepository.mineMealVariability(userJson)
        return VariabilityMiningPreview(
            requestJsonPretty = prettyPrintJson(userJson),
            responseJsonPretty = prettyPrintJson(result.profileJson),
        )
    }

    private fun Record.toObservation(): MealObservation {
        val t = template
        return MealObservation(
            record_id = recordId,
            template_id = t.dbId,
            logged_at = timestamp.toString(),
            title = t.name,
            description = t.description.take(MAX_DESCRIPTION_CHARS),
            notes = t.notes.take(MAX_NOTES_CHARS),
            analysis = if (t.mealComponents.isEmpty()) {
                null
            } else {
                MealObservationAnalysis(
                    components = t.mealComponents.map { c ->
                        MealObservationComponent(
                            name = c.name,
                            estimated_amount = c.estimatedAmount,
                            confidence = c.confidence.toApiString(),
                        )
                    },
                )
            },
            macros = MealObservationMacros(
                calories_kcal = t.nutrients.calories,
                protein_g = t.nutrients.protein,
                fat_g = t.nutrients.fat,
                saturated_fat_g = t.nutrients.ofWhichSaturated,
                carbs_g = t.nutrients.carbs,
                sugars_g = t.nutrients.ofWhichSugar,
                fibre_g = t.nutrients.fibre,
                salt_g = t.nutrients.salt,
            ),
        )
    }

    private fun prettyPrintJson(json: String): String =
        runCatching {
            val el = JsonParser.parseString(json)
            prettyGson.toJson(el)
        }.getOrElse { json }

    private fun ComponentConfidence.toApiString(): String = when (this) {
        ComponentConfidence.HIGH -> "high"
        ComponentConfidence.MEDIUM -> "medium"
        ComponentConfidence.LOW -> "low"
    }
}

private data class VariabilityMiningUserEnvelope(
    @SerializedName("schema_version") val schema_version: String,
    @SerializedName("merge_mode") val merge_mode: String,
    @SerializedName("existing_profile") val existing_profile: Any?,
    @SerializedName("constraints") val constraints: VariabilityConstraints,
    @SerializedName("meal_observations") val meal_observations: List<MealObservation>,
)

private data class VariabilityConstraints(
    @SerializedName("max_archetypes") val max_archetypes: Int,
    @SerializedName("min_evidence_for_high_variability_slot") val min_evidence_for_high_variability_slot: Int,
    /** Slots with fewer distinct variants than this must be omitted entirely. */
    @SerializedName("min_variants_per_slot") val min_variants_per_slot: Int,
    @SerializedName("max_notes_chars_per_entry") val max_notes_chars_per_entry: Int,
    @SerializedName("max_description_chars_per_entry") val max_description_chars_per_entry: Int,
)

private data class MealObservation(
    @SerializedName("record_id") val record_id: Long,
    @SerializedName("template_id") val template_id: Long,
    @SerializedName("logged_at") val logged_at: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("notes") val notes: String,
    @SerializedName("analysis") val analysis: MealObservationAnalysis?,
    @SerializedName("macros") val macros: MealObservationMacros,
)

private data class MealObservationAnalysis(
    @SerializedName("components") val components: List<MealObservationComponent>,
)

private data class MealObservationComponent(
    @SerializedName("name") val name: String,
    @SerializedName("estimated_amount") val estimated_amount: String,
    @SerializedName("confidence") val confidence: String,
)

private data class MealObservationMacros(
    @SerializedName("calories_kcal") val calories_kcal: Int?,
    @SerializedName("protein_g") val protein_g: Float?,
    @SerializedName("fat_g") val fat_g: Float?,
    @SerializedName("saturated_fat_g") val saturated_fat_g: Float?,
    @SerializedName("carbs_g") val carbs_g: Float?,
    @SerializedName("sugars_g") val sugars_g: Float?,
    @SerializedName("fibre_g") val fibre_g: Float?,
    @SerializedName("salt_g") val salt_g: Float?,
)
