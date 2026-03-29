package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.features.common.model.ListUiModelRecord
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal class SharedRecordsUiMapper(
    private val nutrientsUiMapper: NutrientsUiMapper,
) {
    fun map(record: Record, timeOnly: Boolean = false): ListUiModelRecord {
        val timestampStr = mapRecordTimestamp(record.timestamp, timeOnly)

        val nutrients = nutrientsUiMapper.mapRecordNutrients(record.template.nutrients)

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

    private fun mapRecordTimestamp(timestamp: ZonedDateTime, timeOnly: Boolean): String {
        return if (timeOnly) {
            timestamp.formatTimeOnly()
        } else {
            timestamp.format()
        }
    }

    private fun ZonedDateTime.format(): String = format(DateTimeFormatter.ofPattern("dd MMM, H:mm"))
    private fun ZonedDateTime.formatTimeOnly(): String = format(DateTimeFormatter.ofPattern("H:mm"))
}
