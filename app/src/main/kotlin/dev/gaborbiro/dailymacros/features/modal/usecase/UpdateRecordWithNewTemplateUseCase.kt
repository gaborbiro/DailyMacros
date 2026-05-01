package dev.gaborbiro.dailymacros.features.modal.usecase

import androidx.annotation.UiThread
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record

/**
 * Creates a new template and updates the specified record with it.
 * This will delete nutrient data from the record and needs to be AI-d.
 */
internal class UpdateRecordWithNewTemplateUseCase(
    private val repository: RecordsRepository,
    private val createTemplateUseCase: CreateTemplateUseCase,
) {

    @UiThread
    suspend fun execute(recordId: Long, images: List<String>, title: String, description: String) {
        val record: Record = repository.get(recordId)!!
        val parentTemplateId = record.template.dbId
        val newTemplateId = createTemplateUseCase.execute(
            images = images,
            title = title,
            description = description,
            parentTemplateId = parentTemplateId,
        )
        val newTemplate = repository.getTemplate(newTemplateId)
        repository.updateRecord(record.copy(template = newTemplate))
    }
}
