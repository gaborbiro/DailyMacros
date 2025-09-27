package dev.gaborbiro.dailymacros.repo.records.domain.model

import java.time.ZonedDateTime

data class RecordToSave(
    val timestamp: ZonedDateTime,
    val templateToSave: TemplateToSave,
)
