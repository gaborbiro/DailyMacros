package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.data.chatgpt.ChatGPTRepository
import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.data.records.domain.model.Nutrients
import dev.gaborbiro.dailymacros.data.records.domain.model.Record
import dev.gaborbiro.dailymacros.features.common.RecordsMapper
import dev.gaborbiro.dailymacros.features.common.inputStreamToBase64
import dev.gaborbiro.dailymacros.store.bitmap.BitmapStore
import dev.gaborbiro.dailymacros.util.showSimpleNotification

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
        appContext.showSimpleNotification(123L, record.template.name, comment)
    }
}
