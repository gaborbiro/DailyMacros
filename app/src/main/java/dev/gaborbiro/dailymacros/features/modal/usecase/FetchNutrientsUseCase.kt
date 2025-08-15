package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.repo.chatgpt.ChatGPTRepository
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repo.records.domain.model.Nutrients
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.features.common.NutrientsUIMapper
import dev.gaborbiro.dailymacros.features.modal.RecordsMapper
import dev.gaborbiro.dailymacros.features.modal.inputStreamToBase64
import dev.gaborbiro.dailymacros.data.bitmap.ImageStore
import dev.gaborbiro.dailymacros.util.showSimpleNotification

internal class FetchNutrientsUseCase(
    private val appContext: Context,
    private val imageStore: ImageStore,
    private val chatGPTRepository: ChatGPTRepository,
    private val recordsRepository: RecordsRepository,
    private val recordsMapper: RecordsMapper,
    private val nutrientsUIMapper: NutrientsUIMapper,
) {

    suspend fun execute(recordId: Long) {
        val record: Record = recordsRepository.getRecord(recordId)!!
        val base64Image = record.template.image
            ?.let { imageFilename: String ->
                val inputStream = imageStore.get(imageFilename, thumbnail = false)
                inputStreamToBase64(inputStream)
            }
        val response = chatGPTRepository.nutrients(
            request = recordsMapper.mapNutrientsRequest(
                record = record,
                base64Image = base64Image,
            )
        )
        val (nutrients: Nutrients?, issues: String?) = recordsMapper.map(response)
        recordsRepository.updateTemplate(
            templateId = record.template.id,
            nutrients = nutrients,
        )
        val nutrientsStr = nutrientsUIMapper.map(nutrients)
        appContext.showSimpleNotification(
            id = 123L,
            title = null,
            message = listOfNotNull(record.template.name, nutrientsStr, issues).joinToString("\n"),
        )
    }
}
