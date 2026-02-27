package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.App
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.NutrientsUIMapper
import dev.gaborbiro.dailymacros.features.common.workers.GetMacrosWorker
import dev.gaborbiro.dailymacros.features.modal.ModalUIMapper
import dev.gaborbiro.dailymacros.features.modal.inputStreamToBase64
import dev.gaborbiro.dailymacros.features.widgetDiary.DiaryWidgetScreen
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repo.chatgpt.toDomainModel
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.features.common.model.NutrientBreakdown
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.records.domain.model.TopContributors
import dev.gaborbiro.dailymacros.repo.requestStatus.domain.RequestStatusRepository
import dev.gaborbiro.dailymacros.util.showMacroResultsNotification

internal class NutrientAnalysisUseCase(
    private val appContext: Context,
    private val imageStore: ImageStore,
    private val chatGPTRepository: ChatGPTRepository,
    private val recordsRepository: RecordsRepository,
    private val modalUIMapper: ModalUIMapper,
    private val nutrientsUIMapper: NutrientsUIMapper,
    private val requestStatusRepository: RequestStatusRepository,
) {

    suspend fun execute(
        recordId: Long,
    ) {
        val record: Record = recordsRepository.get(recordId)!!
        try {
            val base64Images = record.template.images
                .map { imageFilename: String ->
                    val inputStream = imageStore.open(imageFilename, thumbnail = false)
                    inputStreamToBase64(inputStream)
                }
            requestStatusRepository.markAsPending(record.template.dbId)
            DiaryWidgetScreen.reload()
            val nutrientsResponse = runCatching {
                chatGPTRepository.analyseNutrients(
                    request = modalUIMapper.mapToNutrientAnalysisRequest(
                        record = record,
                        base64Images = base64Images,
                    )
                )
            }

            nutrientsResponse
                .exceptionOrNull()
                ?.let {
                    if (it is ChatGPTApiError.InternetApiError) {
                        GetMacrosWorker.setWorkRequest(
                            appContext = App.appContext,
                            recordId = recordId,
                            force = false,
                        )
                    }
                    throw it
                }

            nutrientsResponse.getOrNull()?.let { nutrientsResponse ->
                val (nutrients: Pair<NutrientBreakdown, TopContributors>?, error: String?) = modalUIMapper.mapNutrientAnalysisResponse(nutrientsResponse)
                recordsRepository.updateTemplate(
                    templateId = record.template.dbId,
                    nutrients = nutrients,
                )
                val cachedTokensMessage = "Cached tokens: ${nutrientsResponse.cachedTokens}"
                nutrients
                    ?.let {
                        val macrosStr = nutrientsUIMapper.mapMacrosPrintout(nutrients.first)
                        appContext.showMacroResultsNotification(
                            id = 123000L + recordId,
                            recordId = recordId,
                            title = null,
                            message = listOfNotNull(record.template.name, macrosStr, error, cachedTokensMessage).joinToString("\n"),
                        )
                    }
                    ?: run {
                        error
                            ?.let {
                                appContext.showMacroResultsNotification(
                                    id = 123000L + recordId,
                                    recordId = recordId,
                                    title = null,
                                    message = listOfNotNull(record.template.name, error, cachedTokensMessage).joinToString("\n"),
                                )
                            }
                            ?: run {
                                appContext.showMacroResultsNotification(
                                    id = 123000L + recordId,
                                    recordId = recordId,
                                    title = null,
                                    message = listOfNotNull(record.template.name, "Something went wrong while fetching macros. Please try again later.", cachedTokensMessage).joinToString("\n"),
                                )
                            }
                    }
            }
        } catch (apiError: ChatGPTApiError) {
            throw apiError
                .toDomainModel()
        } catch (t: Throwable) {
            throw t
        } finally {
            requestStatusRepository.unmark(record.template.dbId)
            DiaryWidgetScreen.reload()
        }
    }
}
