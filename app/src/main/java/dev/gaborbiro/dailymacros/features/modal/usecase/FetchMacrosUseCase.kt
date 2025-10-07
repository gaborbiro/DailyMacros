package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.modal.RecordsMapper
import dev.gaborbiro.dailymacros.features.modal.inputStreamToBase64
import dev.gaborbiro.dailymacros.features.widgetDiary.DiaryWidgetScreen
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repo.chatgpt.toDomainModel
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repo.records.domain.model.Macros
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.requestStatus.domain.RequestStatusRepository
import dev.gaborbiro.dailymacros.util.showMacroResultsNotification

internal class FetchMacrosUseCase(
    private val appContext: Context,
    private val imageStore: ImageStore,
    private val chatGPTRepository: ChatGPTRepository,
    private val recordsRepository: RecordsRepository,
    private val recordsMapper: RecordsMapper,
    private val macrosUIMapper: MacrosUIMapper,
    private val requestStatusRepository: RequestStatusRepository,
) {

    suspend fun execute(recordId: Long) {
        val record: Record = recordsRepository.get(recordId)!!
        try {
            val base64Images = record.template.images
                .map { imageFilename: String ->
                    val inputStream = imageStore.open(imageFilename, thumbnail = false)
                    inputStreamToBase64(inputStream)
                }
            requestStatusRepository.markAsPending(record.template.dbId)
            DiaryWidgetScreen.reload()
            val response = chatGPTRepository.macros(
                request = recordsMapper.mapMacrosRequest(
                    record = record,
                    base64Images = base64Images,
                )
            )
            val (macros: Macros?, issues: String?) = recordsMapper.map(response)
            recordsRepository.updateTemplate(
                templateId = record.template.dbId,
                macros = macros,
            )
            val macrosStr = macrosUIMapper.mapMacrosString(macros)
            appContext.showMacroResultsNotification(
                id = 123000L + recordId,
                recordId = recordId,
                title = null,
                message = listOfNotNull(record.template.name, macrosStr, issues, macros?.notes).joinToString("\n"),
            )
        } catch (apiError: ChatGPTApiError) {
            throw apiError
                .toDomainModel()
        } catch (t: Throwable) {
            throw t // not necessary, but
        } finally {
            requestStatusRepository.unmark(record.template.dbId)
            DiaryWidgetScreen.reload()
        }
    }
}
