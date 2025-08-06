package dev.gaborbiro.nutri.features.modal.usecase

import android.content.Context
import dev.gaborbiro.nutri.data.chatgpt.ChatGPTRepository
import dev.gaborbiro.nutri.data.records.domain.RecordsRepository
import dev.gaborbiro.nutri.data.records.domain.model.Nutrients
import dev.gaborbiro.nutri.data.records.domain.model.Record
import dev.gaborbiro.nutri.features.common.RecordsMapper
import dev.gaborbiro.nutri.features.common.inputStreamToBase64
import dev.gaborbiro.nutri.store.bitmap.BitmapStore
import dev.gaborbiro.nutri.util.showSimpleNotification
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class FetchNutrientsUseCase(
    private val appContext: Context,
    private val bitmapStore: BitmapStore,
    private val chatGPTRepository: ChatGPTRepository,
    private val recordsRepository: RecordsRepository,
    private val mapper: RecordsMapper,
) {

    suspend fun execute(recordId: Long) {
        val record: Record = recordsRepository.getRecord(recordId)!!
        val base64Image = record.template.image
            ?.let { imageFilename: String ->
                val inputStream = bitmapStore.get(imageFilename, thumbnail = false)
                inputStreamToBase64(inputStream)
            }
        val response = chatGPTRepository.nutrients(
            request = mapper.mapNutrientsRequest(
                record = record,
                base64Image = base64Image,
            )
        )
        val (nutrients: Nutrients?, comment) = mapper.map(response)
        recordsRepository.updateTemplate(
            templateId = record.template.id,
            nutrients = nutrients,
        )

        val records = recordsRepository.getRecords(
            LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        )
        val totalCalories = records.sumOf { it.template.nutrients?.calories ?: 0 }
        val totalProtein =
            records.sumOf { it.template.nutrients?.protein?.toDouble() ?: 0.0 }.toInt()
        val totalCarbs =
            records.sumOf { it.template.nutrients?.carbohydrates?.toDouble() ?: 0.0 }.toInt()
        val totalFat = records.sumOf { it.template.nutrients?.fat?.toDouble() ?: 0.0 }.toInt()

        val nutrientsText =
            "Today: Calories: $totalCalories, Protein: $totalProtein, Carbohydrates: $totalCarbs, Fat: $totalFat"

        appContext.showSimpleNotification(111L, nutrientsText)
    }
}
