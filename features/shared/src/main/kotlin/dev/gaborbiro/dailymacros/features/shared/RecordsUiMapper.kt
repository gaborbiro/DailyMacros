package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelRecord
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import javax.inject.Inject

class RecordsUiMapper @Inject constructor(
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
        )
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
