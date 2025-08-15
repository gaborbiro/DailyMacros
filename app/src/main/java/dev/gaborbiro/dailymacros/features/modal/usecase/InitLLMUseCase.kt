package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.data.mlkit.MLKitStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class InitLLMUseCase(
    private val mlKitStore: MLKitStore,
) {
    suspend fun execute() {
        withContext(Dispatchers.IO) {
            mlKitStore.init()
        }
    }
}
