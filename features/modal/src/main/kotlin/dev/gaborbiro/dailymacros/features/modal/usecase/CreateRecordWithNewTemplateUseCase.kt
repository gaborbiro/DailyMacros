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
        images: List<String>,
        title: String,
        description: String,
        parentTemplateId: Long? = null,
    ): Long {
        val templateId = createTemplateUseCase.execute(
            images = images,
            title = title,
            description = description,
            parentTemplateId = parentTemplateId,
        )
        return createRecordFromTemplateUseCase.execute(templateId)
    }
}
