package dev.gaborbiro.dailymacros.repo.records.domain.model

import java.time.ZonedDateTime

data class Record(
    val recordId: Long,
    val timestamp: ZonedDateTime,
    val template: Template,
)
