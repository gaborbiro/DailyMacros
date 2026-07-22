package dev.gaborbiro.dailymacros.features.modal.usecase

import javax.inject.Inject

class ValidateCreateRecordUseCase @Inject constructor() {

    suspend fun execute(
        imageFilenames: List<String>,
        title: String,
        description: String,
    ): CreateValidationResult {
//        if (title.isBlank()) {
//            return CreateValidationResult.Error("Cannot be empty")
//        }
        return CreateValidationResult.Valid
    }
}

sealed class CreateValidationResult {
    data class Error(val message: String) : CreateValidationResult()
    object Valid : CreateValidationResult()
}
