package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.modal.RecordsMapper
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository

internal class ValidateEditRecordUseCase(
    private val repository: RecordsRepository,
    private val mapper: RecordsMapper,
) {

    suspend fun execute(recordId: Long, title: String, description: String): EditValidationResult {
        if (title.isBlank()) {
            return EditValidationResult.Error("Title cannot be empty")
        }
        val template = repository.getRecord(recordId)!!.template
        val templateId = template.dbId
        val records = repository.getRecordsByTemplate(templateId)
        return if (records.size < 2) {
            val confirmNutrientDeletion = template.nutrients != null
            EditValidationResult.Valid(confirmNutrientDeletion)
        } else {
            EditValidationResult.ConfirmMultipleEdit(records.size)
        }
    }
}

sealed class EditValidationResult {
    data class ConfirmMultipleEdit(val count: Int) : EditValidationResult()
    data class Valid(val showNutrientDeletionConfirmationDialog: Boolean) : EditValidationResult()
    data class Error(val message: String) : EditValidationResult()
}
