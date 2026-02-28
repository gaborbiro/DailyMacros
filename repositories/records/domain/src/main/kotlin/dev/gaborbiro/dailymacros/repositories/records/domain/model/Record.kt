package dev.gaborbiro.dailymacros.repositories.records.domain.model

import java.time.ZonedDateTime

data class Record(
    val recordId: Long,
    val timestamp: ZonedDateTime,
    val template: Template,
)
