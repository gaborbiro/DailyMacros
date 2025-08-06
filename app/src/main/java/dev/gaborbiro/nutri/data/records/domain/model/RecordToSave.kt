package dev.gaborbiro.nutri.data.records.domain.model

import java.time.LocalDateTime

data class RecordToSave(
    val timestamp: LocalDateTime,
    val template: TemplateToSave,
)
