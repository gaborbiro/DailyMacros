package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository

internal class ValidateEditImageUseCase(
    private val repository: RecordsRepository,
) {

    suspend fun execute(recordId: Long): EditImageValidationResult {
        val templateId = repository.get(recordId)!!.template.dbId
        val linkedCount = repository.countRecordsForTemplate(templateId)
        return if (linkedCount < 2) {
            EditImageValidationResult.Valid
        } else {
            EditImageValidationResult.AskConfirmation(linkedCount)
        }
    }
}

sealed class EditImageValidationResult {
    class AskConfirmation(val count: Int) : EditImageValidationResult()
    object Valid : EditImageValidationResult()
}
