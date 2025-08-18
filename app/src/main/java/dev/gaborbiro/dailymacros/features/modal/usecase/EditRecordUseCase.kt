package dev.gaborbiro.dailymacros.features.modal.usecase

import android.util.Log
import androidx.annotation.UiThread
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repo.records.domain.model.RecordToSave
import dev.gaborbiro.dailymacros.repo.records.domain.model.TemplateToSave

internal class EditRecordUseCase(
    private val repository: RecordsRepository,
) {

    @UiThread
    suspend fun execute(recordId: Long, title: String, description: String) {
        val record = repository.getRecord(recordId)!!
        repository.deleteRecord(recordId)
        // note: image shouldn't be deleted
        val (templateDeleted, imageDeleted) =
            repository.deleteTemplateIfUnused(record.template.dbId, imageToo = false)
        Log.d("Notes", "template deleted: $templateDeleted, image deleted: $imageDeleted")
        val newRecord = RecordToSave(
            timestamp = record.timestamp,
            template = TemplateToSave(
                primaryImage = record.template.primaryImage,
                name = title,
                description = description,
                // deleting associated macro information because title/description has changed
            ),
        )
        repository.saveRecord(newRecord)
    }
}
