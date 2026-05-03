package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository

internal class ValidateEditRecordUseCase(
    private val repository: RecordsRepository,
) {

    suspend fun execute(recordId: Long, title: String, description: String): EditValidationResult {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            return EditValidationResult.Error("Title cannot be empty")
        }
        val trimmedDescription = description.trim()
        val template = repository.get(recordId)!!.template
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
