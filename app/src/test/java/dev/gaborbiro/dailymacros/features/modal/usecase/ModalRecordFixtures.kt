package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import java.time.ZoneId
import java.time.ZonedDateTime

object ModalRecordFixtures {
    val zone: ZoneId = ZoneId.of("UTC")

    fun template(
        dbId: Long = 1L,
        name: String = "Meal",
        description: String = "D",
        images: List<String> = emptyList(),
    ) = Template(
        dbId = dbId,
        images = images,
        isRepresentativeOfMealByImageIndex = emptyList(),
        name = name,
        description = description,
        parentTemplateId = null,
        createdAtEpochMs = 0L,
        updatedAtEpochMs = 0L,
        isPending = false,
        nutrients = TemplateNutrientBreakdown(),
        notes = "",
        mealComponents = emptyList(),
        topContributors = TopContributors(),
        quickPickOverride = null,
    )

    fun record(recordId: Long, template: Template) = Record(
        recordId = recordId,
        timestamp = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, zone),
        template = template,
    )
}
