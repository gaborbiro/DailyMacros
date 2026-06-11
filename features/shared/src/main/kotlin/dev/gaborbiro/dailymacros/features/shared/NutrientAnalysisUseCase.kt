package dev.gaborbiro.dailymacros.features.shared

import android.content.Context
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.utils.inputStreamToBase64
import dev.gaborbiro.dailymacros.features.shared.model.NutrientBreakdown
import dev.gaborbiro.dailymacros.features.shared.notifications.MacroResultsNotificationSender
import dev.gaborbiro.dailymacros.repositories.chatgpt.BuildConfig
import dev.gaborbiro.dailymacros.repositories.chatgpt.di.ForImageUploadChatGpt
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.MealComponent as ChatGPTMealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.RequestStatusRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.ComponentConfidence
import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent as TemplateMealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import dagger.hilt.android.qualifiers.ApplicationContext
import ellipsize
import javax.inject.Inject

class NutrientAnalysisUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val imageStore: ImageStore,
    @ForImageUploadChatGpt private val chatGPTRepository: ChatGPTRepository,
    private val recordsRepository: RecordsRepository,
    private val requestStatusRepository: RequestStatusRepository,
    private val recordsMapper: RecordsMapper,
    private val macrosNotificationTextMapper: MacrosNotificationTextMapper,
    private val macroResultsNotificationSender: MacroResultsNotificationSender,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun execute(
        recordId: Long,
        /** When true (e.g. [NutrientAnalysisWorker]), show a high-importance notification if the API fails. */
        notifyOnFailure: Boolean = false,
    ) {
        val record: Record = recordsRepository.get(recordId)!!
        try {
            val base64Images = record.template.images
                .map { imageFilename: String ->
                    val inputStream = imageStore.open(imageFilename, thumbnail = false)
                    inputStreamToBase64(inputStream)
                }
            requestStatusRepository.markAsPending(record.template.dbId)

            val nutrientsAnalysisResponse = runCatching {
                chatGPTRepository.analyseNutrients(
                    request = recordsMapper.mapToNutrientAnalysisRequest(
                        record = record,
                        base64Images = base64Images,
                    ).copy(customizations = settingsRepository.getPromptCustomizations())
                )
            }

            nutrientsAnalysisResponse
                .exceptionOrNull()
                ?.let {
                    if (it is ChatGPTDomainError.DisplayMessageToUser.CheckInternetConnection) {
                        NutrientAnalysisWorker.setWorkRequest(
                            appContext = appContext,
                            recordId = recordId,
                            force = false,
                        )
                    }
                    throw it
                }

            nutrientsAnalysisResponse.getOrNull()?.let { nutrientsAnalysisResult ->
                val (nutrients: Pair<NutrientBreakdown, TopContributors>?, error: String?) = recordsMapper.mapNutrientAnalysisResponse(nutrientsAnalysisResult)
                val templateNutrients: Pair<TemplateNutrientBreakdown, TopContributors>? = nutrients?.let {
                    recordsMapper.map(nutrients.first) to nutrients.second
                }
                val name = record.template.name.takeIf { it.isNotBlank() } ?: nutrientsAnalysisResult.title
                val templateImagesWhenSavingMacros = templateNutrients?.let {
                    record.template.images.mapIndexed { index, filename ->
                        TemplateImageUpdate(
                            filename = filename,
                            isRepresentativeOfMeal = nutrientsAnalysisResult.isRepresentativeOfMealByImageIndex.getOrNull(index),
                        )
                    }
                }
                recordsRepository.updateTemplate(
                    templateId = record.template.dbId,
                    // the user can start analysis without having specified a name
                    name = name,
                    templateImages = templateImagesWhenSavingMacros,
                    nutrients = templateNutrients,
                    notes = nutrientsAnalysisResult.notes,
                    mealComponents = templateNutrients?.let { nutrientsAnalysisResult.components.toMealComponents() },
                )
                val cachedTokensMessage = if (BuildConfig.DEBUG) "Cached tokens: ${nutrientsAnalysisResult.cachedTokens}" else null
                nutrients
                    ?.let {
                        val macrosStr = macrosNotificationTextMapper.mapMacrosPrintout(nutrients.first)
                        val message = listOfNotNull(name.ellipsize(50), macrosStr, error, cachedTokensMessage).joinToString("\n").trim()
                        message.takeIf { it.isNotBlank() }?.let {
                            macroResultsNotificationSender.showMacroResultsNotification(
                                id = 123000L + recordId,
                                recordId = recordId,
                                title = null,
                                message = message,
                                isError = false,
                            )
                        }
                    }
                    ?: run {
                        error
                            ?.let {
                                val message = listOfNotNull(name.ellipsize(50), error, cachedTokensMessage).joinToString("\n").trim()
                                message.takeIf { it.isNotBlank() }?.let {
                                    macroResultsNotificationSender.showMacroResultsNotification(
                                        id = 123000L + recordId,
                                        recordId = recordId,
                                        title = null,
                                        message = message,
                                        isError = true,
                                    )
                                }
                            }
                            ?: run {
                                macroResultsNotificationSender.showMacroResultsNotification(
                                    id = 123000L + recordId,
                                    recordId = recordId,
                                    title = null,
                                    message = listOfNotNull(name.ellipsize(50), "Something went wrong while fetching macros. Please try again later.", cachedTokensMessage).joinToString("\n"),
                                    isError = true,
                                )
                            }
                    }
            }
        } catch (domainError: ChatGPTDomainError) {
            if (notifyOnFailure) {
                macroResultsNotificationSender.showMacroResultsNotification(
                    id = 123000L + recordId,
                    recordId = recordId,
                    title = "Couldn't fetch macros",
                    message = macrosNotificationTextMapper.mapDomainErrorToUserMessage(domainError),
                    isError = true,
                )
            }
            throw domainError
        } catch (t: Throwable) {
            if (notifyOnFailure) {
                val message = t.message
                    ?: t.cause?.message
                    ?: "Something went wrong while fetching macros. Please try again later."
                macroResultsNotificationSender.showMacroResultsNotification(
                    id = 123000L + recordId,
                    recordId = recordId,
                    title = "Couldn't fetch macros",
                    message = message,
                    isError = true,
                )
            }
            throw t
        } finally {
            requestStatusRepository.unmark(record.template.dbId)
        }
    }
}

private fun List<ChatGPTMealComponent>.toMealComponents(): List<TemplateMealComponent> =
    map { component ->
        TemplateMealComponent(
            name = component.name,
            estimatedAmount = component.estimatedAmount,
            confidence = when (component.confidence.lowercase()) {
                "medium" -> ComponentConfidence.MEDIUM
                "low" -> ComponentConfidence.LOW
                else -> ComponentConfidence.HIGH
            },
        )
    }
