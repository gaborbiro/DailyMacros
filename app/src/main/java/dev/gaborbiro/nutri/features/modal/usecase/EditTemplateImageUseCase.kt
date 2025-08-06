package dev.gaborbiro.nutri.features.modal.usecase

import dev.gaborbiro.nutri.data.records.domain.RecordsRepository
import dev.gaborbiro.nutri.features.common.BaseUseCase

class EditTemplateImageUseCase(
    private val repository: RecordsRepository,
) : BaseUseCase() {

    suspend fun execute(recordId: Long, uri: String?) {
        val templateId = repository.getRecord(recordId)!!.template.id
        repository.updateTemplate(templateId, uri)
    }
}
