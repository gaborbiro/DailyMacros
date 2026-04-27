package dev.gaborbiro.dailymacros.features.modal.usecase

import androidx.annotation.UiThread
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record

/**
 * Creates a new template and updates the specified record with it.
 * This will delete nutrient data from the record and needs to be AI-d.
 *
 * [coverPhotoByImageIndex] is forwarded to [CreateTemplateUseCase] so new `template_images` rows
 * can store food-recognition `coverPhoto` when the user edits images and saves (same path as create).
 */
internal class UpdateRecordWithNewTemplateUseCase(
    private val repository: RecordsRepository,
    private val createTemplateUseCase: CreateTemplateUseCase,
) {

    @UiThread
    suspend fun execute(
        recordId: Long,
        images: List<String>,
        title: String,
        description: String,
        coverPhotoByImageIndex: List<Boolean?> = emptyList(),
    ) {
        val record: Record = repository.get(recordId)!!
        val newTemplateId = createTemplateUseCase.execute(
            images = images,
            title = title,
            description = description,
            coverPhotoByImageIndex = coverPhotoByImageIndex,
        )
        val newTemplate = repository.getTemplate(newTemplateId)
        repository.updateRecord(record.copy(template = newTemplate))
    }
}
