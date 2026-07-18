package dev.gaborbiro.dailymacros.features.modal.usecase

import androidx.annotation.UiThread
import dev.gaborbiro.dailymacros.features.shared.CreateRecordFromTemplateUseCase
import javax.inject.Inject

class CreateRecordWithNewTemplateUseCase @Inject constructor(
    private val createTemplateUseCase: CreateTemplateUseCase,
    private val createRecordFromTemplateUseCase: CreateRecordFromTemplateUseCase,
) {

    @UiThread
    suspend fun execute(
        imageFilenames: List<String>,
        title: String,
        description: String,
        parentTemplateId: Long? = null,
        imageSourceMediaStoreIds: Map<String, Long> = emptyMap(),
    ): Long {
        val templateId = createTemplateUseCase.execute(
            imageFilenames = imageFilenames,
            title = title,
            description = description,
            parentTemplateId = parentTemplateId,
            imageSourceMediaStoreIds = imageSourceMediaStoreIds,
        )
        return createRecordFromTemplateUseCase.execute(templateId)
    }
}
