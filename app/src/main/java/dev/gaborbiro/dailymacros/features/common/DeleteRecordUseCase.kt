package dev.gaborbiro.dailymacros.features.common

import android.util.Log
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository

internal class DeleteRecordUseCase(
    private val repository: RecordsRepository,
) {

    suspend fun execute(recordId: Long) {
        val record = repository.deleteRecord(recordId)
        val (templateDeleted, imageDeleted) =
            repository.deleteTemplateIfUnused(templateId = record.template.dbId, imageToo = true)
        Log.d("Notes", "template deleted: $templateDeleted, image deleted: $imageDeleted")
    }
}
