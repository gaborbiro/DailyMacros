package dev.gaborbiro.dailymacros.features.modal.usecase

import android.util.Log
import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.data.records.domain.model.RecordToSave
import dev.gaborbiro.dailymacros.data.records.domain.model.TemplateToSave

internal class EditRecordImageUseCase(
    private val repository: RecordsRepository,
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
        repository.deleteRecord(recordId)
        val (templateDeleted, imageDeleted) =
            repository.deleteTemplateIfUnused(record.template.id)
        Log.d("Notes", "template deleted: $templateDeleted, image deleted: $imageDeleted")
    }
}
