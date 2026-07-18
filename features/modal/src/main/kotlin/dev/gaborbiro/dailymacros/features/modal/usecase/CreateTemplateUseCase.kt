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
        imageFilenames: List<String>,
        title: String,
        description: String,
        parentTemplateId: Long? = null,
        imageSourceMediaStoreIds: Map<String, Long> = emptyMap(),
    ): Long {
        val template = TemplateToSave(
            imageFilenames = imageFilenames,
            name = title,
            description = description,
            parentTemplateId = parentTemplateId,
            imageSourceMediaStoreIds = imageSourceMediaStoreIds,
        )
        return recordsRepository.saveTemplate(template)
    }
}
