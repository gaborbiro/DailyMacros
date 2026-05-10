package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.features.common.workers.GetMacrosWorker
import dev.gaborbiro.dailymacros.features.modal.model.ChangeImagesTarget
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository

/**
 * Applies the shared-template edit confirmation: update either the single record or the backing
 * template, then schedules macro re-analysis for that record.
 */
internal class ApplyConfirmedSharedTemplateEditUseCase(
    private val updateRecordWithNewTemplateUseCase: UpdateRecordWithNewTemplateUseCase,
    private val editTemplateUseCase: EditTemplateUseCase,
    private val recordsRepository: RecordsRepository,
    private val appContext: Context,
) {

    suspend fun execute(
        target: ChangeImagesTarget,
        recordId: Long,
        images: List<String>,
        title: String,
        description: String,
    ) {
        when (target) {
            ChangeImagesTarget.RECORD -> {
                updateRecordWithNewTemplateUseCase.execute(
                    recordId = recordId,
                    images = images,
                    title = title,
                    description = description,
                )
            }

            ChangeImagesTarget.TEMPLATE -> {
                val templateId = recordsRepository.get(recordId)!!.template.dbId
                editTemplateUseCase.execute(
                    templateId = templateId,
                    images = images,
                    title = title,
                    description = description,
                )
            }
        }
        GetMacrosWorker.setWorkRequest(
            appContext = appContext,
            recordId = recordId,
            force = true,
        )
    }
}
