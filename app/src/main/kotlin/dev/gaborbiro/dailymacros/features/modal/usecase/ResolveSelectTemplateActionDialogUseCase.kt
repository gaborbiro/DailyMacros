package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import javax.inject.Inject

class ResolveSelectTemplateActionDialogUseCase @Inject constructor(
    private val recordsRepository: RecordsRepository,
) {

    suspend fun execute(templateId: Long): DialogHandle.SelectTemplateActionDialog {
        val title =
            recordsRepository.getRecordsByTemplate(templateId).firstOrNull()?.template?.name ?: ""
        return DialogHandle.SelectTemplateActionDialog(templateId, title)
    }
}
