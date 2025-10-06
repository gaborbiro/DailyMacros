package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository

internal class EditTemplateUseCase(
    private val repository: RecordsRepository,
) {

    suspend fun execute(
        templateId: Long,
        images: List<String>,
        title: String,
        description: String,
    ) {
        repository.updateTemplate(
            templateId = templateId,
            title = title,
            description = description,
            images = images,
        )
    }
}
