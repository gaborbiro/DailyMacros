package dev.gaborbiro.dailymacros.features.modal.usecase

import android.util.Log
import androidx.annotation.UiThread
import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.data.records.domain.model.RecordToSave
import dev.gaborbiro.dailymacros.data.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.features.common.BaseUseCase

class EditRecordUseCase(
    private val repository: RecordsRepository,
) : BaseUseCase() {

    @UiThread
    suspend fun execute(recordId: Long, title: String, description: String) {
        val record = repository.getRecord(recordId)!!
        repository.deleteRecord(recordId)
        // note: image shouldn't be deleted
        val (templateDeleted, imageDeleted) =
            repository.deleteTemplateIfUnused(record.template.id, imageToo = false)
        Log.d("Notes", "template deleted: $templateDeleted, image deleted: $imageDeleted")
        val newRecord = RecordToSave(
            timestamp = record.timestamp,
            template = TemplateToSave(
                image = record.template.image,
                name = title,
                description = description,
            ),
        )
        repository.saveRecord(newRecord)
    }
}
