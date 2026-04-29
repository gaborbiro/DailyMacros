package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate

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
            name = title,
            description = description,
            templateImages = images.map { TemplateImageUpdate(filename = it) },
        )
    }
}
