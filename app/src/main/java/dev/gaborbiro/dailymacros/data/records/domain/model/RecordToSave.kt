package dev.gaborbiro.dailymacros.data.records.domain.model

import java.time.LocalDateTime

data class RecordToSave(
    val timestamp: LocalDateTime,
    val template: TemplateToSave,
)
