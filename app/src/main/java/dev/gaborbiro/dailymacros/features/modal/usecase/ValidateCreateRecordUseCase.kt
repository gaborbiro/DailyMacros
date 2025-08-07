package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.common.BaseUseCase

class ValidateCreateRecordUseCase : BaseUseCase() {

    suspend fun execute(image: String?, title: String, description: String): CreateValidationResult {
        if (title.isBlank()) {
            return CreateValidationResult.Error("Cannot be empty")
        }
        return CreateValidationResult.Valid
    }
}

sealed class CreateValidationResult {
    data class Error(val message: String) : CreateValidationResult()
    object Valid : CreateValidationResult()
}
