package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import java.time.ZonedDateTime
import javax.inject.Inject

class ListMealVariantsForTemplateUseCase @Inject constructor(
    private val recordsRepository: RecordsRepository,
) {

    suspend fun execute(currentTemplateId: Long): MealVariantListResult? {
        val familyIds = recordsRepository.getTemplateIdsInSameVariantFamily(currentTemplateId)
        if (familyIds.isEmpty()) return null

        val siblingIds = familyIds.filter { it != currentTemplateId }
            .filter { recordsRepository.countRecordsForTemplate(it) > 0 }

        val currentTemplate = recordsRepository.getTemplate(currentTemplateId)
        val currentLast = latestRecordTimeForTemplate(currentTemplateId)
        val zone = currentLast?.zone ?: ZonedDateTime.now().zone
        val epochStart = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, zone)
        val others = buildList {
            for (tid in siblingIds) {
                val template = runCatching { recordsRepository.getTemplate(tid) }.getOrNull()
                add(
                    MealVariantListRow(
                        templateId = tid,
                        title = (template?.name ?: "").ifBlank { "Meal #$tid" },
                        lastUsed = latestRecordTimeForTemplate(tid),
                        isCurrent = false,
                        componentNames = template?.mealComponents?.map { it.name } ?: emptyList(),
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
                title = currentTemplate.name.ifBlank { "Meal #$currentTemplateId" },
                lastUsed = currentLast,
                isCurrent = true,
                componentNames = currentTemplate.mealComponents.map { it.name },
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
