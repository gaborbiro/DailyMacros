package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.modal.model.MealVariantListResult
import dev.gaborbiro.dailymacros.features.modal.model.MealVariantListRow
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Lists sibling meal templates created in the same **user** variant family as [currentTemplateId]
 * (`parentTemplateId` / fork links only; no AI or title matching).
 */
class ListMealVariantsForTemplateUseCase @Inject constructor(
    private val recordsRepository: RecordsRepository,
) {

    suspend fun execute(currentTemplateId: Long): MealVariantListResult? {
        val familyIds = recordsRepository.getTemplateIdsInSameVariantFamily(currentTemplateId)
        if (familyIds.isEmpty()) return null

        val siblingIds = familyIds.filter { it != currentTemplateId }
            .filter { recordsRepository.countRecordsForTemplate(it) > 0 }

        val currentTitle = recordsRepository.getTemplate(currentTemplateId).name
        val currentLast = latestRecordTimeForTemplate(currentTemplateId)
        val zone = currentLast?.zone ?: ZonedDateTime.now().zone
        val epochStart = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, zone)
        val others = buildList {
            for (tid in siblingIds) {
                val title = runCatching { recordsRepository.getTemplate(tid).name }.getOrElse { "" }
                add(
                    MealVariantListRow(
                        templateId = tid,
                        title = title.ifBlank { "Meal #$tid" },
                        lastUsed = latestRecordTimeForTemplate(tid),
                        isCurrent = false,
                    ),
                )
            }
        }.sortedWith(
            compareByDescending<MealVariantListRow> { it.lastUsed ?: epochStart }
                .thenBy { it.title },
        )

        return MealVariantListResult(
            current = MealVariantListRow(
                templateId = currentTemplateId,
                title = currentTitle.ifBlank { "Meal #$currentTemplateId" },
                lastUsed = currentLast,
                isCurrent = true,
            ),
            others = others,
        )
    }

    suspend fun hasOtherVariants(currentTemplateId: Long): Boolean {
        val familyIds = recordsRepository.getTemplateIdsInSameVariantFamily(currentTemplateId)
        return familyIds.any { tid ->
            tid != currentTemplateId && recordsRepository.countRecordsForTemplate(tid) > 0
        }
    }

    private suspend fun latestRecordTimeForTemplate(templateId: Long): ZonedDateTime? =
        recordsRepository.getRecordsByTemplate(templateId)
            .maxOfOrNull { it.timestamp }
}
