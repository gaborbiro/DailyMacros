package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.features.common.workers.GetMacrosWorker

internal class DeleteRecordWithWorkerCleanupUseCase(
    private val deleteRecordUseCase: DeleteRecordUseCase,
    private val appContext: Context,
) {

    suspend fun execute(recordId: Long) {
        deleteRecordUseCase.execute(recordId)
        GetMacrosWorker.cancelWorkRequest(
            appContext = appContext,
            recordId = recordId,
        )
    }
}
