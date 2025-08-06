package dev.gaborbiro.nutri.features.modal.usecase

import dev.gaborbiro.nutri.data.records.domain.RecordsRepository
import dev.gaborbiro.nutri.features.common.BaseUseCase

class EditTemplateUseCase(
    private val repository: RecordsRepository
) : BaseUseCase() {

    suspend fun execute(recordId: Long, title: String, description: String) {
        val templateId = repository.getRecord(recordId)!!.template.id
        repository.updateTemplate(
            templateId = templateId,
            title = title,
            description = description
        )
    }
}
