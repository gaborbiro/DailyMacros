package dev.gaborbiro.dailymacros.features.settings.export.useCases

import com.google.gson.GsonBuilder
import dev.gaborbiro.dailymacros.features.settings.export.CreatePublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.export.SharePublicUriLauncher
import dev.gaborbiro.dailymacros.features.settings.export.StreamWriter
import dev.gaborbiro.dailymacros.features.common.utils.diaryDayStartTime
import dev.gaborbiro.dailymacros.features.common.utils.logicalDiaryDate
import dev.gaborbiro.dailymacros.features.common.utils.logicalDiaryToday
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import java.io.OutputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

class ExportFoodDiaryUseCase @Inject constructor(
    private val recordRepository: RecordsRepository,
    private val streamWriter: StreamWriter,
    private val sharePublicUriLauncher: SharePublicUriLauncher,
    private val settingsRepository: SettingsRepository,
) {

    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .serializeNulls()
        .setPrettyPrinting()
        .create()

    suspend fun execute(createPublicDocumentUseCase: CreatePublicDocumentUseCase) {
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

        val dayStart = diaryDayStartTime(settingsRepository.getDiaryDayStartHour())
        val zone = ZoneId.systemDefault()
        val from = records.minOf { it.timestamp }.logicalDiaryDate(dayStart)
        val to = maxOf(
            logicalDiaryToday(zone, dayStart),
            records.maxOf { it.timestamp }.logicalDiaryDate(dayStart),
        )
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
    val exportedAt: String,
    val entries: List<LlmFoodEntry>,
)

private data class LlmFoodEntry(
    val timestamp: String,
    val title: String,
    val description: String,
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
