package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.data.bitmap.ImageStore

internal class GetRecordImageUseCase(
    private val repository: RecordsRepository,
    private val imageStore: ImageStore,
) {

    suspend fun execute(recordId: Long, thumbnail: Boolean): DialogState.ViewImageDialog? {
        val template = repository.getRecord(recordId)!!.template
        return template
            .image
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
