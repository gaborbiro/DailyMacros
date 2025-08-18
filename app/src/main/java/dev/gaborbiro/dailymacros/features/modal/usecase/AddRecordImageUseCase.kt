package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repo.records.domain.model.RecordToSave
import dev.gaborbiro.dailymacros.repo.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.features.common.DeleteRecordUseCase

internal class AddRecordImageUseCase(
    private val repository: RecordsRepository,
    private val deleteRecordUseCase: DeleteRecordUseCase,
) {

    suspend fun execute(recordId: Long, filename: String) {
        val record = repository.getRecord(recordId)!!
        val newRecord = RecordToSave(
            timestamp = record.timestamp,
            templateToSave = TemplateToSave(
                images = record.template.images + filename,
                name = record.template.name,
                description = record.template.description,
            ),
        )
        repository.saveRecord(newRecord)
        deleteRecordUseCase.execute(recordId)
    }
}
