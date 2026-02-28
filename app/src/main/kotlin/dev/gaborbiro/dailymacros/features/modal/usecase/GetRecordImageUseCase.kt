package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository

internal class GetRecordImageUseCase(
    private val repository: RecordsRepository,
) {

    suspend fun execute(recordId: Long): DialogHandle.ViewImageDialog? {
        val template = repository.get(recordId)!!.template
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
