package dev.gaborbiro.dailymacros.features.settings

import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.RetrofillParentLineageResult

/**
 * One-off: sets [parentTemplateId] from shared image filenames (strong signal only).
 */
class RetrofillParentTemplateIdsUseCase(
    private val recordsRepository: RecordsRepository,
) {
    suspend fun execute(): RetrofillParentLineageResult =
        recordsRepository.retrofillParentTemplateIdsFromSharedImages()
}
