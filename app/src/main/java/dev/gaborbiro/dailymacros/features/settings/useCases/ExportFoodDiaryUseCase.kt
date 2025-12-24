package dev.gaborbiro.dailymacros.features.settings.useCases

import com.google.gson.GsonBuilder
import dev.gaborbiro.dailymacros.features.main.CreateJsonDocumentUseCase
import dev.gaborbiro.dailymacros.features.main.ShareIntentLauncher
import dev.gaborbiro.dailymacros.features.main.StreamWriter
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import java.time.ZonedDateTime

internal class ExportFoodDiaryUseCase(
    private val recordRepository: RecordsRepository,
    private val createJsonDocumentUseCase: CreateJsonDocumentUseCase,
    private val streamWriter: StreamWriter,
    private val shareIntentLauncher: ShareIntentLauncher,
) {

    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .serializeNulls()
        .setPrettyPrinting()
        .create()

    suspend fun execute() {
        val (fileName, diary) = generatePayload()
        val uri = createJsonDocumentUseCase.execute(fileName)
        uri?.let {
            streamWriter.execute(uri) { output ->
                val bytes = gson.toJson(diary).toByteArray(Charsets.UTF_8)
                output.write(bytes)
                output.flush()
            }
            shareIntentLauncher.execute(
                uri = uri,
                chooserTitle = "Share food diary with ChatGPT",
            )
        }
    }

    private suspend fun generatePayload(): Pair<String, LlmFoodDiary> {
        val records = recordRepository
            .getRecords()
        val entries = records
            .mapNotNull {
                it.template.macros
                    ?.let { macros ->
                        LlmFoodEntry(
                            timestamp = it.timestamp.toString(),
                            title = it.template.name,
                            description = it.template.description,
                            calories_kcal = macros.calories,
                            protein_g = macros.protein,
                            fat_g = macros.fat,
                            saturated_fat_g = macros.ofWhichSaturated,
                            carbs_g = macros.carbs,
                            sugars_g = macros.ofWhichSugar,
                            fibre_g = macros.fibre,
                            salt_g = macros.salt,
                            notes = macros.notes,
                        )
                    }
            }

        val from = records.minOf { it.timestamp }
        val to = records.maxOf { it.timestamp }
        val fileName =
            "food-diary-${from.toLocalDate()}_to_${to.toLocalDate()}.json"

        val diary = LlmFoodDiary(
            exportedAt = ZonedDateTime.now().toString(),
            entries = entries,
        )

        return fileName to diary
    }
}

private data class LlmFoodDiary(
    val schemaVersion: Int = 1,
    val exportedAt: String,          // ISO-8601
    val entries: List<LlmFoodEntry>,
)

private data class LlmFoodEntry(
    val timestamp: String,          // ISO-8601
    val title: String,               // template.name
    val description: String,         // template.description
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
