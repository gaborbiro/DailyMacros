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
            .groupBy { it.timestamp.toLocalDate() }
            .map { (day, records) ->
                listOf(
                    macrosUIMapper.mapMacroProgressTable(records, targets, day)
                ) + records.map {
                    map(it, showDay)
                }
            }
            .flatten()
    }

    fun map(
        records: List<Record>,
        showDay: Boolean,
    ): List<ListUIModelBase> {
        return records
            .groupBy { it.timestamp.toLocalDate() }
            .map { (_, records) ->
                records.map {
                    map(it, showDay)
                }
            }
            .flatten()
    }


    private fun map(record: Record, forceDay: Boolean): ListUIModelRecord {
        val timestampStr = dateUIMapper.map(record.timestamp, forceDay)

        val macros = record.template.macros
            ?.let { macrosUIMapper.mapMacros(it) }

        return ListUIModelRecord(
            recordId = record.dbId,
            templateId = record.template.dbId,
            images = record.template.images,
            timestamp = timestampStr,
            title = record.template.name,
            macros = macros,
        )
    }
}
