package dev.gaborbiro.dailymacros.features.modal.usecase

import androidx.annotation.UiThread
import dev.gaborbiro.dailymacros.features.common.CreateRecordFromTemplateUseCase

internal class CreateRecordWithNewTemplateUseCase(
    private val createTemplateUseCase: CreateTemplateUseCase,
    private val createRecordFromTemplateUseCase: CreateRecordFromTemplateUseCase,
) {

    @UiThread
    suspend fun execute(
        images: List<String>,
        title: String,
        description: String,
    ): Long {
        val templateId = createTemplateUseCase.execute(
            images = images,
            title = title,
            description = description,
        )
        return createRecordFromTemplateUseCase.execute(templateId)
    }
}
