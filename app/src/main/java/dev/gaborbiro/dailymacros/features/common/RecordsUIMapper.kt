package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.features.common.model.BaseListItemUIModel
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.util.formatShort
import dev.gaborbiro.dailymacros.util.formatShortTime
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal class RecordsUIMapper(
    private val macrosUIMapper: MacrosUIMapper,
) {
    fun map(records: List<Record>): List<BaseListItemUIModel> {
        return records
            .groupBy { it.timestamp.toLocalDate() }
            .map { (day, records) ->
                listOf(
                    macrosUIMapper.mapMacroProgressTable(records, day)
                ) + records.map {
                    map(it)
                }
            }
            .flatten()
    }


    private fun map(record: Record): RecordUIModel {
        val timestamp = record.timestamp
        val timestampStr = when {
            !timestamp.isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)) -> {
                "Today at ${timestamp.formatShortTime()}"
            }

            !timestamp.isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(1)) -> {
                "Yesterday at ${timestamp.formatShortTime()}"
            }

            else -> timestamp.formatShort()
        }
        val macros = record.template.macros
            ?.let { macrosUIMapper.mapMacros(it) }

        return RecordUIModel(
            recordId = record.dbId,
            templateId = record.template.dbId,
            images = record.template.images,
            timestamp = timestampStr,
            title = record.template.name,
            macros = macros,
        )
    }
}
