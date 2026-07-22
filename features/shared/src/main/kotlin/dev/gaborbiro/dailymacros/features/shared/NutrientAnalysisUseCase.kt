package dev.gaborbiro.dailymacros.features.shared

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.shared.R
import dev.gaborbiro.dailymacros.features.common.utils.inputStreamToBase64
import dev.gaborbiro.dailymacros.features.shared.notifications.MacroResultsNotificationSender
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ForImageUploadChatGpt
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.RequestStatusRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.ComponentConfidence
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import ellipsize
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.MealComponent as ChatGPTMealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent as TemplateMealComponent

class NutrientAnalysisUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val imageStore: ImageStore,
    @ForImageUploadChatGpt private val chatGPTRepository: ChatGPTRepository,
    private val recordsRepository: RecordsRepository,
    private val requestStatusRepository: RequestStatusRepository,
    private val errorUiMapper: ErrorUiMapper,
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
            val base64Images = record.template.imageFilenames
                .map { imageFilename: String ->
                    val inputStream = imageStore.open(imageFilename, thumbnail = false)
                    inputStreamToBase64(inputStream)
                }
            requestStatusRepository.markAsPending(record.template.dbId)

            val nutrientsAnalysisResponse = runCatching {
                chatGPTRepository.analyseNutrients(
                    request = NutrientAnalysisRequest(
                        base64Images = base64Images,
                        title = record.template.name,
                        description = record.template.description,
                        customisations = settingsRepository.getEffectiveCustomisations(),
                        phoneLanguage = Locale.getDefault().getDisplayLanguage(Locale.ENGLISH),
                    )
                )
            }

            nutrientsAnalysisResponse
                .exceptionOrNull()
                ?.let {
                    if (it is DomainError.DisplayMessageToUser.CheckInternetConnection) {
                        NutrientAnalysisWorker.setWorkRequest(
                            appContext = appContext,
                            recordId = recordId,
                            force = false,
                        )
                    }
                    throw it
                }

            nutrientsAnalysisResponse.getOrNull()?.let { nutrientsAnalysisResult ->
                val nutrients = nutrientsAnalysisResult.nutrients
                val error = nutrientsAnalysisResult.error
                val templateNutrients = nutrients?.let {
                    it to nutrientsAnalysisResult.topContributors!!
                }
                val name = record.template.name.takeIf { it.isNotBlank() } ?: nutrientsAnalysisResult.title?.takeIf { it.isNotBlank() }
                val templateImagesWhenSavingMacros = templateNutrients?.let {
                    record.template.imageFilenames.mapIndexed { index, filename ->
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
                nutrients
                    ?.let {
                        macroResultsNotificationSender.showMacroResultsNotification(
                            id = 123000L + recordId,
                            recordId = recordId,
                            title = name.ellipsize(50) ?: appContext.getString(R.string.shared_content_analysis_success_title_fallback),
                            message = appContext.getString(R.string.shared_content_analysis_completed),
                            isError = false,
                        )
                    }
                    ?: run {
                        if (error == null) {
                            macroResultsNotificationSender.showMacroResultsNotification(
                                id = 123000L + recordId,
                                recordId = recordId,
                                title = name.ellipsize(50)
                                    ?.let { appContext.getString(R.string.shared_content_analysis_error_title, it) }
                                    ?: appContext.getString(R.string.shared_content_analysis_error_title_unknown),
                                message = appContext.getString(R.string.shared_content_please_try_again),
                                isError = true,
                            )
                        }
                    }
                error
                    ?.let {
                        val message = listOfNotNull(name.ellipsize(50), error).joinToString("\n").trim()
                        message.takeIf { it.isNotBlank() }?.let {
                            macroResultsNotificationSender.showMacroResultsNotification(
                                id = 123001L + recordId,
                                recordId = recordId,
                                title = name.ellipsize(50)
                                    ?.let { appContext.getString(R.string.shared_content_analysis_error_title, it) }
                                    ?: appContext.getString(R.string.shared_content_analysis_error_title_unknown),
                                message = message,
                                isError = true,
                            )
                        }
                    }
            }
        } catch (domainError: DomainError) {
            if (notifyOnFailure) {
                val foodName = record.template.name.takeIf { it.isNotBlank() }
                macroResultsNotificationSender.showMacroResultsNotification(
                    id = 123000L + recordId,
                    recordId = recordId,
                    title = foodName?.ellipsize(50)
                        ?.let { appContext.getString(R.string.shared_content_macros_error_title, it) }
                        ?: appContext.getString(R.string.shared_content_macros_error_title_unknown),
                    message = errorUiMapper.mapErrorMessage(domainError, appContext.getString(R.string.shared_content_please_try_again)),
                    isError = true,
                )
            }
            throw domainError
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            if (notifyOnFailure) {
                val foodName = record.template.name.takeIf { it.isNotBlank() }
                macroResultsNotificationSender.showMacroResultsNotification(
                    id = 123000L + recordId,
                    recordId = recordId,
                    title = foodName?.ellipsize(50)
                        ?.let { appContext.getString(R.string.shared_content_macros_error_title, it) }
                        ?: appContext.getString(R.string.shared_content_macros_error_title_unknown),
                    message = appContext.getString(R.string.shared_content_please_try_again),
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
