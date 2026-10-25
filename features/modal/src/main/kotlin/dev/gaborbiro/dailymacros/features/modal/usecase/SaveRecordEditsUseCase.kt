package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.features.modal.MealEditRules
import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot
import dev.gaborbiro.dailymacros.features.shared.NutrientAnalysisWorker
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import javax.inject.Inject

/**
 * Saves edits to a logged meal and schedules a new nutrient analysis when the wording or the set of
 * photos changed. A meal with logged variants is updated in place (every log of it is re-analyzed);
 * otherwise this log gets its own template, unless nothing needs re-analyzing (reordered photos,
 * whitespace around the wording), which updates the template in place and keeps its nutrients.
 */
class SaveRecordEditsUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val recordsRepository: RecordsRepository,
    private val updateRecordWithNewTemplateUseCase: UpdateRecordWithNewTemplateUseCase,
) {

    /**
     * @param pristine the meal as it was before these edits
     * @param knownToHaveLoggedVariants true when the caller already knows the meal has other logged
     * variants (its variant picker shows), which saves looking it up
     * @param imageRepresentativeFlags keyed by filename, so flags follow their photo when reordered
     */
    suspend fun execute(
        recordId: Long,
        templateId: Long,
        pristine: RecordDetailsPristineSnapshot,
        knownToHaveLoggedVariants: Boolean,
        title: String,
        description: String,
        imageFilenames: List<String>,
        imageRepresentativeFlags: Map<String, Boolean?>,
    ) {
        val requiresAnalysis = MealEditRules.editsRequireMacroAnalysis(
            pristine = pristine,
            title = title,
            description = description,
            imageFilenames = imageFilenames,
        )
        val templateImages = imageFilenames.map { filename ->
            TemplateImageUpdate(
                filename = filename,
                isRepresentativeOfMeal = imageRepresentativeFlags[filename],
            )
        }
        if (knownToHaveLoggedVariants || hasLoggedVariants(templateId)) {
            recordsRepository.updateTemplate(
                templateId = templateId,
                name = title,
                description = description,
                templateImages = templateImages,
            )
            if (requiresAnalysis) {
                scheduleAnalysisForTemplate(templateId)
            }
        } else {
            // Only edits that need a new analysis get their own template: the new template starts
            // without nutrients, so forking without analysing would lose them.
            if (!requiresAnalysis) {
                recordsRepository.updateTemplate(
                    templateId = templateId,
                    name = title,
                    description = description,
                    templateImages = templateImages,
                )
            } else {
                updateRecordWithNewTemplateUseCase.execute(
                    recordId = recordId,
                    imageFilenames = imageFilenames,
                    title = title,
                    description = description,
                )
                if (requiresAnalysis) {
                    NutrientAnalysisWorker.setWorkRequest(
                        appContext = appContext,
                        recordId = recordId,
                        force = true,
                        wifiOnly = false,
                    )
                }
            }
        }
    }

    private suspend fun hasLoggedVariants(templateId: Long): Boolean {
        val familyIds = recordsRepository.getTemplateIdsInSameVariantFamily(templateId)
        return familyIds.count { recordsRepository.countRecordsForTemplate(it) > 0 } > 1
    }

    /**
     * Analysis results are stored on the shared template, so every record using it is updated by a
     * single run. Scheduling one job per record would repeat the same API call and notify N times.
     */
    private suspend fun scheduleAnalysisForTemplate(templateId: Long) {
        val record = recordsRepository.getRecordsByTemplate(templateId).firstOrNull() ?: return
        NutrientAnalysisWorker.setWorkRequest(
            appContext = appContext,
            recordId = record.recordId,
            force = true,
            wifiOnly = false,
        )
    }
}
