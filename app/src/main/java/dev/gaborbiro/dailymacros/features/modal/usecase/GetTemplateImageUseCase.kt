package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository

internal class GetTemplateImageUseCase(
    private val repository: RecordsRepository,
) {

    suspend fun execute(templateId: Long): DialogHandle.ViewImageDialog? {
        val template = repository.getTemplate(templateId)
        return template.images
            .takeIf { it.isNotEmpty() }
            ?.let {
                DialogHandle.ViewImageDialog(
                    title = template.name,
                    images = it,
                )
            }
    }
}
