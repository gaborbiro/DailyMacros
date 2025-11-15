package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.features.common.model.ListUIModelRecord
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record

internal class RecordsUIMapper(
    private val macrosUIMapper: MacrosUIMapper,
    private val dateUIMapper: DateUIMapper,
) {
    fun map(record: Record, forceDay: Boolean): ListUIModelRecord {
        val timestampStr = dateUIMapper.mapRecordTimestamp(record.timestamp, forceDay)

        val macros = record.template.macros
            ?.let { macrosUIMapper.mapMacroAmounts(it) }

        return ListUIModelRecord(
            recordId = record.recordId,
            templateId = record.template.dbId,
            images = record.template.images,
            timestamp = timestampStr,
            title = record.template.name,
            macrosAmounts = macros,
            showLoadingIndicator = record.template.isPending,
        )
    }
}
