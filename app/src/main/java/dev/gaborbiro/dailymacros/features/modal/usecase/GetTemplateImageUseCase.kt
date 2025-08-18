package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository

internal class GetTemplateImageUseCase(
    private val repository: RecordsRepository,
    private val imageStore: ImageStore,
) {

    suspend fun execute(templateId: Long, thumbnail: Boolean): DialogState.ViewImageDialog? {
        val template = repository.getTemplate(templateId)!!
        return template
            .primaryImage
            ?.let {
                imageStore.read(it, thumbnail)
            }
            ?.let {
                DialogState.ViewImageDialog(
                    title = template.name,
                    bitmap = it
                )
            }
    }
}
