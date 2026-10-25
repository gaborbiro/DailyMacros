package dev.gaborbiro.dailymacros.features.modal.usecase

import androidx.annotation.UiThread
import dev.gaborbiro.dailymacros.features.shared.CreateRecordFromTemplateUseCase
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Logs a quick pick as a new meal right now (the "Log meal again" action, from the widget or its
 * confirmation dialog), queueing AI analysis if the meal hasn't been analysed yet.
 */
class LogMealFromTemplateUseCase @Inject constructor(
    private val createRecordFromTemplateUseCase: CreateRecordFromTemplateUseCase,
    private val scheduleAnalysisIfTemplateIncompleteUseCase: ScheduleAnalysisIfTemplateIncompleteUseCase,
) {

    @UiThread
    suspend fun execute(templateId: Long) {
        val recordId = createRecordFromTemplateUseCase.execute(
            templateId,
            ZonedDateTime.now(ZoneId.systemDefault()),
        )
        scheduleAnalysisIfTemplateIncompleteUseCase.execute(recordId, templateId)
    }
}
