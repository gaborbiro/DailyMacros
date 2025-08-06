package dev.gaborbiro.nutri.features.modal.usecase

import android.util.Log
import dev.gaborbiro.nutri.data.records.domain.RecordsRepository
import dev.gaborbiro.nutri.data.records.domain.model.RecordToSave
import dev.gaborbiro.nutri.data.records.domain.model.TemplateToSave
import dev.gaborbiro.nutri.features.common.BaseUseCase

class EditRecordImageUseCase(
    private val repository: RecordsRepository,
) : BaseUseCase() {

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
