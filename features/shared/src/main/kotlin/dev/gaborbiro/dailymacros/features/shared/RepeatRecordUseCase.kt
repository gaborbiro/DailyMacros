package dev.gaborbiro.dailymacros.features.shared

import androidx.annotation.UiThread
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository

import javax.inject.Inject

class RepeatRecordUseCase @Inject constructor(
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
