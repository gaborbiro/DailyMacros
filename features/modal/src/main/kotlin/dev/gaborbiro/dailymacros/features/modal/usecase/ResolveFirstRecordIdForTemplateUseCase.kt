package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import javax.inject.Inject

class ResolveFirstRecordIdForTemplateUseCase @Inject constructor(
    private val recordsRepository: RecordsRepository,
) {

    suspend fun execute(templateId: Long): Long? =
        recordsRepository.getRecordsByTemplate(templateId).firstOrNull()?.recordId
}
