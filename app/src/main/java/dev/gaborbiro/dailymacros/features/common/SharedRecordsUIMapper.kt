package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.features.common.model.ListUiModelRecord
import dev.gaborbiro.dailymacros.features.common.model.NutrientBreakdown
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template

internal class SharedRecordsUIMapper(
    private val nutrientsUIMapper: NutrientsUIMapper,
    private val dateUIMapper: DateUIMapper,
) {
    fun map(record: Record, timeOnly: Boolean = false): ListUiModelRecord {
        val timestampStr = dateUIMapper.mapRecordTimestamp(record.timestamp, timeOnly)

        val nutrients = nutrientsUIMapper.map(NutrientBreakdown.fromTemplate(record.template.nutrients))

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
