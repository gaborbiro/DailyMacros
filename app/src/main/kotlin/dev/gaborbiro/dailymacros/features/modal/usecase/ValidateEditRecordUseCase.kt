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
        repository.get(recordId)
            ?: return EditValidationResult.Error("Record not found")
        return EditValidationResult.Valid
    }
}

sealed class EditValidationResult {
    data object Valid : EditValidationResult()
    data class Error(val message: String) : EditValidationResult()
}
