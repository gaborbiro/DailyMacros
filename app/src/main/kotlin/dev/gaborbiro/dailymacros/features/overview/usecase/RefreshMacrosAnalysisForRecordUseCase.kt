package dev.gaborbiro.dailymacros.features.overview.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.features.common.workers.GetMacrosWorker

internal class RefreshMacrosAnalysisForRecordUseCase(
    private val appContext: Context,
) {

    suspend fun execute(recordId: Long) {
        GetMacrosWorker.cancelWorkRequest(
            appContext = appContext,
            recordId = recordId,
        )
        GetMacrosWorker.setWorkRequest(
            appContext = appContext,
            recordId = recordId,
            force = true,
        )
    }
}
