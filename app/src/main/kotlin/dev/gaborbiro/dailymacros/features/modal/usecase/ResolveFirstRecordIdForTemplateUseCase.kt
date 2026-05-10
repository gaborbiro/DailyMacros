package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository

internal class ResolveFirstRecordIdForTemplateUseCase(
    private val recordsRepository: RecordsRepository,
) {

    suspend fun execute(templateId: Long): Long? =
        recordsRepository.getRecordsByTemplate(templateId).firstOrNull()?.recordId
}
