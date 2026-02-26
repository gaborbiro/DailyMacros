package dev.gaborbiro.dailymacros.features.settings.export.useCases

import com.google.gson.GsonBuilder
import dev.gaborbiro.dailymacros.features.settings.export.CreatePublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.export.SharePublicUriLauncher
import dev.gaborbiro.dailymacros.features.settings.export.StreamWriter
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import java.io.OutputStream
import java.time.ZonedDateTime

internal class ExportFoodDiaryUseCase(
    private val recordRepository: RecordsRepository,
    private val createPublicDocumentUseCase: CreatePublicDocumentUseCase,
    private val streamWriter: StreamWriter,
    private val sharePublicUriLauncher: SharePublicUriLauncher,
) {

    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .serializeNulls()
        .setPrettyPrinting()
        .create()

    suspend fun execute() {
        val (fileName, diary) = generatePayload()
        val uri = createPublicDocumentUseCase.execute(fileName)
        uri?.let {
            streamWriter.execute(uri) { output: OutputStream ->
                val bytes = gson.toJson(diary).toByteArray(Charsets.UTF_8)
                output.write(bytes)
                output.flush()
            }
            sharePublicUriLauncher.execute(uri)
        }
    }

    private suspend fun generatePayload(): Pair<String, LlmFoodDiary> {
        val records = recordRepository
            .getRecords()
        val entries = records
            .map {
                val nutrients = it.template.nutrients
                LlmFoodEntry(
                    timestamp = it.timestamp.toString(),
                    title = it.template.name,
                    description = it.template.description,
                    calories_kcal = nutrients.calories,
                    protein_g = nutrients.protein,
                    fat_g = nutrients.fat,
                    saturated_fat_g = nutrients.ofWhichSaturated,
                    carbs_g = nutrients.carbs,
                    sugars_g = nutrients.ofWhichSugar,
                    fibre_g = nutrients.fibre,
                    salt_g = nutrients.salt,
                    notes = it.template.notes,
                )
            } 

        val from = records.minOf { it.timestamp }.toLocalDate()
        val to = ZonedDateTime.now().toLocalDate()
        val fileName =
            "food-diary-${from}_to_${to}.json"

        val diary = LlmFoodDiary(
            exportedAt = ZonedDateTime.now().toString(),
            entries = entries,
        )

        return fileName to diary
    }
}

private data class LlmFoodDiary(
    val schemaVersion: Int = 1,
    val exportedAt: String,         // ISO-8601
    val entries: List<LlmFoodEntry>,
)

private data class LlmFoodEntry(
    val timestamp: String,          // ISO-8601
    val title: String,              // template.name
    val description: String,        // template.description
    val calories_kcal: Int?,
    val protein_g: Float?,
    val fat_g: Float?,
    val saturated_fat_g: Float?,
    val carbs_g: Float?,
    val sugars_g: Float?,
    val fibre_g: Float?,
    val salt_g: Float?,
    val notes: String?,
)
