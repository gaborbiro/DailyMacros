package dev.gaborbiro.dailymacros.features.overview.usecase

import android.util.Log
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository

/**
 * Removes a template if no records reference it (e.g. after the user dismisses undo delete).
 */
internal class DeleteUnusedTemplateIfOrphanedUseCase(
    private val recordsRepository: RecordsRepository,
) {

    suspend fun execute(templateId: Long) {
        val (templateDeleted, imageDeleted) = recordsRepository.deleteTemplateIfUnused(
            templateId = templateId,
            imageToo = true,
        )
        Log.d(
            "Notes",
            "template deleted: $templateDeleted, image deleted: $imageDeleted"
        )
    }
}
