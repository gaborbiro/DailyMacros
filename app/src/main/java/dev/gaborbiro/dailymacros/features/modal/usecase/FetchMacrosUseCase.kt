package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.modal.RecordsMapper
import dev.gaborbiro.dailymacros.features.modal.inputStreamToBase64
import dev.gaborbiro.dailymacros.features.widget.NotesWidget
import dev.gaborbiro.dailymacros.repo.chatgpt.ChatGPTRepository
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repo.records.domain.model.Macros
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.util.showSimpleNotification

internal class FetchMacrosUseCase(
    private val appContext: Context,
    private val imageStore: ImageStore,
    private val chatGPTRepository: ChatGPTRepository,
    private val recordsRepository: RecordsRepository,
    private val recordsMapper: RecordsMapper,
    private val macrosUIMapper: MacrosUIMapper,
) {

    suspend fun execute(recordId: Long) {
        val record: Record = recordsRepository.getRecord(recordId)!!
        val base64Image = record.template.primaryImage
            ?.let { imageFilename: String ->
                val inputStream = imageStore.open(imageFilename, thumbnail = false)
                inputStreamToBase64(inputStream)
            }
        val response = chatGPTRepository.macros(
            request = recordsMapper.mapMacrosRequest(
                record = record,
                base64Image = base64Image,
            )
        )
        val (macros: Macros?, issues: String?) = recordsMapper.map(response)
        recordsRepository.updateTemplate(
            templateId = record.template.dbId,
            macros = macros,
        )
        val macrosStr = macrosUIMapper.map(macros)
        appContext.showSimpleNotification(
            id = 123L,
            title = null,
            message = listOfNotNull(record.template.name, macrosStr, issues, macros?.notes).joinToString("\n"),
        )
        NotesWidget.reload()
    }
}
