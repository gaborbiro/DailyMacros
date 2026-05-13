package dev.gaborbiro.dailymacros.features.overview.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.features.common.workers.NutrientAnalysisWorker
import javax.inject.Inject

class CancelMacrosAnalysisForRecordUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    fun execute(recordId: Long) {
        NutrientAnalysisWorker.cancelWorkRequest(
            appContext = appContext,
            recordId = recordId,
        )
    }
}
