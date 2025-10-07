package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.features.common.model.ListUIModelBase
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelRecord
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.settings.model.Targets

internal class RecordsUIMapper(
    private val macrosUIMapper: MacrosUIMapper,
    private val dateUIMapper: DateUIMapper,
) {
    fun map(
        records: List<Record>,
        targets: Targets,
        showDay: Boolean,
    ): List<ListUIModelBase> {
        return records
            .groupByWallClockDay()
            .reversed()
            .map { travelDay ->
                listOf(
                    macrosUIMapper.mapMacroProgressTable(
                        day = travelDay,
                        targets = targets,
                    )
                ) + travelDay.records.reversed().map {
                    map(it, showDay)
                }
            }
            .flatten()
    }

    fun map(
        records: List<Record>,
        showDay: Boolean,
    ): List<ListUIModelBase> {
        return records.map {
            map(it, showDay)
        }
    }


    private fun map(record: Record, forceDay: Boolean): ListUIModelRecord {
        val timestampStr = dateUIMapper.map(record.timestamp, forceDay)

        val macros = record.template.macros
            ?.let { macrosUIMapper.mapMacros(it) }

        return ListUIModelRecord(
            recordId = record.recordId,
            templateId = record.template.dbId,
            images = record.template.images,
            timestamp = timestampStr,
            title = record.template.name,
            macros = macros,
            showLoadingIndicator = record.template.isPending,
        )
    }

    private fun List<Record>.groupByWallClockDay(): List<TravelDay> {
        return this
            .sortedBy { it.timestamp.toInstant() }
            .groupByTo(
                destination = sortedMapOf(),
                keySelector = { it.timestamp.toLocalDate() },
                valueTransform = { it }
            )
            .map { (day, records) ->
                val start = records.minBy { it.timestamp }.timestamp.zone
                val end = records.maxBy { it.timestamp }.timestamp.zone
                TravelDay(
                    records = records.toList(),
                    day = day,
                    startZone = start,
                    endZone = end,
                )
            }
    }
}
