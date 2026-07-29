package dev.gaborbiro.dailymacros.features.modal.usecase

import androidx.annotation.UiThread
import dev.gaborbiro.dailymacros.features.shared.CreateRecordFromTemplateUseCase
import java.time.ZonedDateTime
import javax.inject.Inject

class CreateRecordWithNewTemplateUseCase @Inject constructor(
    private val createTemplateUseCase: CreateTemplateUseCase,
    private val createRecordFromTemplateUseCase: CreateRecordFromTemplateUseCase,
) {

    /**
     * [timestamp] must be captured by the caller when the user started assembling this record,
     * not when this use case runs (see [CreateRecordFromTemplateUseCase.execute]).
     */
    @UiThread
    suspend fun execute(
        imageFilenames: List<String>,
        title: String,
        description: String,
        timestamp: ZonedDateTime,
        parentTemplateId: Long? = null,
    ): Long {
        val templateId = createTemplateUseCase.execute(
            imageFilenames = imageFilenames,
            title = title,
            description = description,
            parentTemplateId = parentTemplateId,
        )
        return createRecordFromTemplateUseCase.execute(templateId, timestamp)
    }
}
