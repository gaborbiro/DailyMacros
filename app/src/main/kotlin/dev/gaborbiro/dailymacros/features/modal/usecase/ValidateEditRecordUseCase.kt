package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import javax.inject.Inject

class ValidateEditRecordUseCase @Inject constructor(
    private val repository: RecordsRepository,
) {

    suspend fun execute(recordId: Long, title: String, description: String): EditValidationResult {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            return EditValidationResult.Error("Title cannot be empty")
        }
        val trimmedDescription = description.trim()
        val record = repository.get(recordId)
            ?: return EditValidationResult.Error("Record not found")
        val template = record.template
        val templateId = template.dbId
        val linkedCount = repository.countRecordsForTemplate(templateId)
        if (linkedCount < 2) {
            return EditValidationResult.Valid
        }
        val templateTextChanged =
            trimmedTitle != template.name.trim() || trimmedDescription != template.description.trim()
        return if (templateTextChanged) {
            EditValidationResult.ConfirmMultipleEdit(linkedCount)
        } else {
            EditValidationResult.Valid
        }
    }
}

sealed class EditValidationResult {
    data class ConfirmMultipleEdit(val count: Int) : EditValidationResult()
    data object Valid : EditValidationResult()
    data class Error(val message: String) : EditValidationResult()
}
