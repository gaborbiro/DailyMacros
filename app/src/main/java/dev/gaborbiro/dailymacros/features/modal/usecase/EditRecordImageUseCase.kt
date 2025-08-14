package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.data.records.domain.model.RecordToSave
import dev.gaborbiro.dailymacros.data.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.features.common.DeleteRecordUseCase

internal class EditRecordImageUseCase(
    private val repository: RecordsRepository,
    private val deleteRecordUseCase: DeleteRecordUseCase,
) {

    suspend fun execute(recordId: Long, filename: String?) {
        val record = repository.getRecord(recordId)!!
        val newRecord = RecordToSave(
            timestamp = record.timestamp,
            template = TemplateToSave(
                image = filename,
                name = record.template.name,
                description = record.template.description,
            ),
        )
        repository.saveRecord(newRecord)
        deleteRecordUseCase.execute(recordId)
    }
}
