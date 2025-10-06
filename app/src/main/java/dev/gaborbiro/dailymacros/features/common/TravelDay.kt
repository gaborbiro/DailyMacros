package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import java.time.LocalDate
import java.time.ZoneId

data class TravelDay(
    val records: List<Record>,
    val day: LocalDate,
    val start: ZoneId,
    val end: ZoneId,
)
