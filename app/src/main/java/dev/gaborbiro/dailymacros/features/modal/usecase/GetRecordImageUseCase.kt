package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository

internal class GetRecordImageUseCase(
    private val repository: RecordsRepository,
    private val imageStore: ImageStore,
) {

    suspend fun execute(recordId: Long, thumbnail: Boolean): DialogHandle.ViewImageDialog? {
        val template = repository.get(recordId)!!.template
        return template
            .primaryImage
            ?.let {
                imageStore.read(it, thumbnail)
            }
            ?.let {
                DialogHandle.ViewImageDialog(
                    title = template.name,
                    bitmap = it
                )
            }
    }
}
