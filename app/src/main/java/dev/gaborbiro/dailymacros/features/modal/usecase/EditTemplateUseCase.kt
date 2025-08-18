package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository

internal class EditTemplateUseCase(
    private val repository: RecordsRepository,
) {

    suspend fun execute(recordId: Long, title: String, description: String) {
        val templateId = repository.getRecord(recordId)!!.template.dbId
        repository.updateTemplate(
            templateId = templateId,
            title = title,
            description = description
        )
    }
}
