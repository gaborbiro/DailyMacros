package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.features.common.BaseUseCase

class EditTemplateImageUseCase(
    private val repository: RecordsRepository,
) : BaseUseCase() {

    suspend fun execute(recordId: Long, uri: String?) {
        val templateId = repository.getRecord(recordId)!!.template.id
        repository.updateTemplate(templateId, uri)
    }
}
