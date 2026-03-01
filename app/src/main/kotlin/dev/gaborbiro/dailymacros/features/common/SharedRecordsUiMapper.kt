package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.features.common.model.ListUiModelRecord
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template

internal class SharedRecordsUiMapper(
    private val nutrientsUiMapper: NutrientsUiMapper,
    private val dateUiMapper: DateUiMapper,
) {
    fun map(record: Record, timeOnly: Boolean = false): ListUiModelRecord {
        val timestampStr = dateUiMapper.mapRecordTimestamp(record.timestamp, timeOnly)

        val nutrients = nutrientsUiMapper.map(record.template.nutrients)

        return ListUiModelRecord(
            recordId = record.recordId,
            templateId = record.template.dbId,
            images = record.template.images,
            timestamp = timestampStr,
            title = record.template.name,
            nutrients = nutrients,
            showLoadingIndicator = record.template.isPending,
            showAddToQuickPicksMenuItem = mapShowAddToQuickPicksMenuItem(record.template.quickPickOverride),
        )
    }

    private fun mapShowAddToQuickPicksMenuItem(quickPickOverride: Template.QuickPickOverride?): Boolean {
        return when (quickPickOverride) {
            Template.QuickPickOverride.INCLUDE -> false
            Template.QuickPickOverride.EXCLUDE -> true
            null -> true
        }
    }
}
