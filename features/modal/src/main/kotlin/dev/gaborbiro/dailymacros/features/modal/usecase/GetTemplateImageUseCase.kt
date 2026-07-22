package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import javax.inject.Inject

class GetTemplateImageUseCase @Inject constructor(
    private val repository: RecordsRepository,
) {

    suspend fun execute(templateId: Long): DialogHandle.ViewImageDialog? {
        val template = repository.getTemplate(templateId)
        return template.imageFilenames
            .takeIf { it.isNotEmpty() }
            ?.let {
                DialogHandle.ViewImageDialog(
                    title = template.name,
                    imageFilenames = it,
                )
            }
    }
}
