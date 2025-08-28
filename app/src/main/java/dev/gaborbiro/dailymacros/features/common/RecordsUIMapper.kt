package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.features.common.model.ListUIModelBase
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelRecord
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record

internal class RecordsUIMapper(
    private val macrosUIMapper: MacrosUIMapper,
    private val dateUIMapper: DateUIMapper,
) {
    fun map(records: List<Record>): List<ListUIModelBase> {
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


    private fun map(record: Record): ListUIModelRecord {
        val timestampStr = dateUIMapper.map(record.timestamp)

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
