package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository

internal class ValidateEditImageUseCase(
    private val repository: RecordsRepository,
) {

    suspend fun execute(recordId: Long): EditImageValidationResult {
        val templateId = repository.getRecord(recordId)!!.template.id
        val records = repository.getRecordsByTemplate(templateId)
        return if (records.size < 2) {
            EditImageValidationResult.Valid
        } else {
            EditImageValidationResult.AskConfirmation(records.size)
        }
    }
}

sealed class EditImageValidationResult {
    class AskConfirmation(val count: Int) : EditImageValidationResult()
    object Valid : EditImageValidationResult()
}
