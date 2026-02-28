package dev.gaborbiro.dailymacros.features.modal.usecase

class ValidateCreateRecordUseCase {

    suspend fun execute(
        images: List<String>,
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
