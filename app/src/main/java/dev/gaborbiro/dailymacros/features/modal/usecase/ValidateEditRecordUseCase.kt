package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository

internal class ValidateEditRecordUseCase(
    private val repository: RecordsRepository,
) {

    suspend fun execute(recordId: Long, title: String, description: String): EditValidationResult {
        if (title.isBlank()) {
            return EditValidationResult.Error("Title cannot be empty")
        }
        val templateId = repository.getRecord(recordId)!!.template.id
        val records = repository.getRecordsByTemplate(templateId)
        return if (records.size < 2) {
            EditValidationResult.Valid
        } else {
            EditValidationResult.ConfirmMultipleEdit(records.size)
        }
    }
}

sealed class EditValidationResult {
    class ConfirmMultipleEdit(val count: Int) : EditValidationResult()
    object Valid : EditValidationResult()
    data class Error(val message: String) : EditValidationResult()
}
