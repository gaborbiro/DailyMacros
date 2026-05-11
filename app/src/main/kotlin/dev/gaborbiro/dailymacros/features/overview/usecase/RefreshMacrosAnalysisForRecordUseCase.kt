package dev.gaborbiro.dailymacros.features.overview.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.features.common.workers.GetMacrosWorker
import javax.inject.Inject

class RefreshMacrosAnalysisForRecordUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
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
