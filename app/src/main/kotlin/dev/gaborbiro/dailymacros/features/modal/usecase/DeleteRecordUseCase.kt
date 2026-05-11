package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import android.util.Log
import dev.gaborbiro.dailymacros.features.common.workers.GetMacrosWorker
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeleteRecordUseCase @Inject constructor(
    private val repository: RecordsRepository,
    @ApplicationContext private val appContext: Context,
) {

    suspend fun execute(recordId: Long) {
        val record = repository.deleteRecord(recordId)
        val (templateDeleted, imageDeleted) =
            repository.deleteTemplateIfUnused(templateId = record.template.dbId, imageToo = true)
        Log.d("Notes", "template deleted: $templateDeleted, image deleted: $imageDeleted")
        GetMacrosWorker.cancelWorkRequest(
            appContext = appContext,
            recordId = recordId,
        )
    }
}
