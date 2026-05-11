package dev.gaborbiro.dailymacros.features.modal.usecase

import androidx.annotation.UiThread
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import javax.inject.Inject

class CreateTemplateUseCase @Inject constructor(
    private val recordsRepository: RecordsRepository,
) {

    @UiThread
    suspend fun execute(
        images: List<String>,
        title: String,
        description: String,
        parentTemplateId: Long? = null,
    ): Long {
        val template = TemplateToSave(
            images = images,
            name = title,
            description = description,
            parentTemplateId = parentTemplateId,
        )
        return recordsRepository.saveTemplate(template)
    }
}
