package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.store.bitmap.BitmapStore

internal class GetTemplateImageUseCase(
    private val repository: RecordsRepository,
    private val bitmapStore: BitmapStore,
) {

    suspend fun execute(templateId: Long, thumbnail: Boolean): DialogState.ViewImageDialog? {
        val template = repository.getTemplate(templateId)!!
        return template
            .image
            ?.let {
                bitmapStore.read(it, thumbnail)
            }
            ?.let {
                DialogState.ViewImageDialog(
                    title = template.name,
                    bitmap = it
                )
            }
    }
}
