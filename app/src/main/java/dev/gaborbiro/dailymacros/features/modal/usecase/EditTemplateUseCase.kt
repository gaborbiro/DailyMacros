package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository

internal class EditTemplateUseCase(
    private val repository: RecordsRepository,
) {

    suspend fun execute(recordId: Long, images: List<String>, title: String, description: String) {
        val templateId = repository.get(recordId)!!.template.dbId
        repository.updateTemplate(
            templateId = templateId,
            title = title,
            description = description,
            images = images,
        )
    }
}
