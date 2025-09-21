package dev.gaborbiro.dailymacros.features.modal.usecase

import android.util.Log
import androidx.annotation.UiThread
import androidx.work.WorkManager
import dev.gaborbiro.dailymacros.App
import dev.gaborbiro.dailymacros.features.common.workers.MacrosWorkRequest
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repo.records.domain.model.RecordToSave
import dev.gaborbiro.dailymacros.repo.records.domain.model.TemplateToSave

internal class EditRecordUseCase(
    private val repository: RecordsRepository,
) {

    @UiThread
    suspend fun execute(recordId: Long, images: List<String>, title: String, description: String) {
        val record = repository.get(recordId)!!
        repository.deleteRecord(recordId)
        // note: image shouldn't be deleted
        val (templateDeleted, imageDeleted) =
            repository.deleteTemplateIfUnused(record.template.dbId, imageToo = false)
        Log.d("Notes", "template deleted: $templateDeleted, image deleted: $imageDeleted")
        val newRecord = RecordToSave(
            timestamp = record.timestamp,
            templateToSave = TemplateToSave(
                images = images,
                name = title,
                description = description,
                // deleting associated macro information because title/description has changed
            ),
        )
        val newRecordId = repository.saveRecord(newRecord)

        WorkManager.getInstance(App.appContext).enqueue(
            MacrosWorkRequest.getWorkRequest(
                recordId = newRecordId
            )
        )
    }
}
