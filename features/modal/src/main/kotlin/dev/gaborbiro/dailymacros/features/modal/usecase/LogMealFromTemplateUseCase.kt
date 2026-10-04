package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import androidx.annotation.UiThread
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.features.shared.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.shared.NutrientAnalysisWorker
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Logs a quick pick as a new meal right now (the "Log meal again" action, from the widget or its
 * confirmation dialog), queueing AI analysis if the meal hasn't been analysed yet.
 */
class LogMealFromTemplateUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val recordsRepository: RecordsRepository,
    private val createRecordFromTemplateUseCase: CreateRecordFromTemplateUseCase,
) {

    @UiThread
    suspend fun execute(templateId: Long) {
        val recordId = createRecordFromTemplateUseCase.execute(
            templateId,
            ZonedDateTime.now(ZoneId.systemDefault()),
        )
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
