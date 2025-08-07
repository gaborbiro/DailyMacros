package dev.gaborbiro.dailymacros.data.records.domain.model

import java.time.LocalDateTime

data class Record(
    val id: Long,
    val timestamp: LocalDateTime,
    val template: Template,
)
