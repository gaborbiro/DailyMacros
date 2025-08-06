package dev.gaborbiro.nutri.data.records.domain.model

import java.time.LocalDateTime

data class Record(
    val id: Long,
    val timestamp: LocalDateTime,
    val template: Template,
)
