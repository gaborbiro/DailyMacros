package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import javax.inject.Inject

class ApplyQuickPickOverrideAndReloadWidgetUseCase @Inject constructor(
    private val recordsRepository: RecordsRepository,
) {

    suspend fun execute(templateId: Long, override: Template.QuickPickOverride) {
        recordsRepository.addQuickPickOverride(templateId, override)
    }
}
