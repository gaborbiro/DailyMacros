package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository

internal class EditTemplateImageUseCase(
    private val repository: RecordsRepository,
) {

    suspend fun execute(recordId: Long, uri: String?) {
        val templateId = repository.getRecord(recordId)!!.template.dbId
        repository.updateTemplate(templateId, uri)
    }
}
