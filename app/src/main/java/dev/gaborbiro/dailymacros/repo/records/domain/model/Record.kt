package dev.gaborbiro.dailymacros.repo.records.domain.model

import java.time.ZonedDateTime

data class Record(
    val dbId: Long,
    val timestamp: ZonedDateTime,
    val template: Template,
)
