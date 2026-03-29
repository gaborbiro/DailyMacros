package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.App
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.RecordsMapper
import dev.gaborbiro.dailymacros.features.common.model.NutrientBreakdown
import dev.gaborbiro.dailymacros.features.common.workers.GetMacrosWorker
import dev.gaborbiro.dailymacros.features.modal.inputStreamToBase64
import dev.gaborbiro.dailymacros.features.widget.DiaryWidgetScreen
import dev.gaborbiro.dailymacros.repositories.chatgpt.BuildConfig
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repositories.chatgpt.toDomainModel
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.RequestStatusRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import dev.gaborbiro.dailymacros.util.showMacroResultsNotification

internal class NutrientAnalysisUseCase(
    private val appContext: Context,
    private val imageStore: ImageStore,
    private val chatGPTRepository: ChatGPTRepository,
    private val recordsRepository: RecordsRepository,
    private val requestStatusRepository: RequestStatusRepository,
    private val recordsMapper: RecordsMapper,
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
                    request = recordsMapper.mapToNutrientAnalysisRequest(
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
                val (nutrients: Pair<NutrientBreakdown, TopContributors>?, error: String?) = recordsMapper.mapNutrientAnalysisResponse(nutrientsResponse)
                val templateNutrients: Pair<TemplateNutrientBreakdown, TopContributors>? = nutrients?.let {
                    recordsMapper.map(nutrients.first) to nutrients.second
                }
                recordsRepository.updateTemplate(
                    templateId = record.template.dbId,
                    // the user can start analysis without having specified a name
                    name = record.template.name.takeIf { it.isNotBlank() } ?: nutrientsResponse.title,
                    nutrients = templateNutrients,
                    notes = nutrientsResponse.notes,
                )
                val cachedTokensMessage = if (BuildConfig.DEBUG) "Cached tokens: ${nutrientsResponse.cachedTokens}" else null
                nutrients
                    ?.let {
                        val macrosStr = recordsMapper.mapMacrosPrintout(nutrients.first)
                        appContext.showMacroResultsNotification(
                            id = 123000L + recordId,
                            recordId = recordId,
                            title = null,
                            message = listOfNotNull(record.template.name, macrosStr, error, cachedTokensMessage).joinToString("\n"),
                            isError = false,
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
                                    isError = true,
                                )
                            }
                            ?: run {
                                appContext.showMacroResultsNotification(
                                    id = 123000L + recordId,
                                    recordId = recordId,
                                    title = null,
                                    message = listOfNotNull(record.template.name, "Something went wrong while fetching macros. Please try again later.", cachedTokensMessage).joinToString("\n"),
                                    isError = true,
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
