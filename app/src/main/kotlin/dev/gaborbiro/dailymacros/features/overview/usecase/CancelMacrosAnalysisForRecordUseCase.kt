package dev.gaborbiro.dailymacros.features.overview.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.features.common.workers.GetMacrosWorker

internal class CancelMacrosAnalysisForRecordUseCase(
    private val appContext: Context,
) {

    fun execute(recordId: Long) {
        GetMacrosWorker.cancelWorkRequest(
            appContext = appContext,
            recordId = recordId,
        )
    }
}
