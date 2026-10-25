package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.features.shared.NutrientAnalysisWorker
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import javax.inject.Inject

/**
 * Queues a nutrient analysis for a record logged from an existing meal, when that meal hasn't been
 * analysed yet (still pending, or no calories).
 */
class ScheduleAnalysisIfTemplateIncompleteUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val recordsRepository: RecordsRepository,
) {

    suspend fun execute(recordId: Long, templateId: Long) {
        val template = recordsRepository.getTemplate(templateId)
        if (template.isPending || template.nutrients.calories == null) {
            NutrientAnalysisWorker.setWorkRequest(
                appContext = appContext,
                recordId = recordId,
                force = true,
                wifiOnly = false,
            )
        }
    }
}
