package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository

internal class ResolveSelectRecordActionDialogUseCase(
    private val recordsRepository: RecordsRepository,
) {

    suspend fun execute(recordId: Long): DialogHandle.SelectRecordActionDialog {
        val title = recordsRepository.get(recordId)?.template?.name ?: ""
        return DialogHandle.SelectRecordActionDialog(recordId, title)
    }
}
