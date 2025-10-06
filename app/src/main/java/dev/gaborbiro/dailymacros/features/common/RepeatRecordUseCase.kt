package dev.gaborbiro.dailymacros.features.common

import androidx.annotation.UiThread
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository

internal class RepeatRecordUseCase(
    private val recordsRepository: RecordsRepository,
    private val createRecordFromTemplateUseCase: CreateRecordFromTemplateUseCase,
) {

    @UiThread
    suspend fun execute(
        recordId: Long,
    ): Long {
        val record = recordsRepository.get(recordId)!!
        val templateId = record.template.dbId
        return createRecordFromTemplateUseCase.execute(templateId)
    }
}
