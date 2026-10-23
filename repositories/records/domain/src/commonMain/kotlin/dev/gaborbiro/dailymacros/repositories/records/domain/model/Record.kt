package dev.gaborbiro.dailymacros.repositories.records.domain.model

import dev.gaborbiro.dailymacros.repositories.common.model.ZonedInstant

data class Record(
    val recordId: Long,
    val timestamp: ZonedInstant,
    val template: Template,
)
