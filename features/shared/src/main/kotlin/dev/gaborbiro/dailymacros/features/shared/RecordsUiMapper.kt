package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelRecord
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

import javax.inject.Inject

class RecordsUiMapper @Inject constructor(
    private val templateUiMapper: TemplateUiMapper,
) {
    fun map(record: Record, timeOnly: Boolean = false, previousRecord: Record? = null): ListUiModelRecord {
        val timestampStr = mapRecordTimestamp(record.timestamp, timeOnly) +
            (timezoneShiftLabel(previousRecord, record)?.let { " $it" } ?: "")

        val nutrients = templateUiMapper.mapRecordNutrients(record.template.nutrients)

        return ListUiModelRecord(
            recordId = record.recordId,
            templateId = record.template.dbId,
            imageFilename = templateUiMapper.getBestPhoto(record.template),
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

    /**
     * Label like "(+1h)" or "(-1h30m)" describing how much the wall-clock offset shifted
     * between [previousRecord] and [record], shown only when the two were logged in different
     * timezones (e.g. crossing timezones mid-flight).
     */
    private fun timezoneShiftLabel(previousRecord: Record?, record: Record): String? {
        val previousZone = previousRecord?.timestamp?.zone ?: return null
        if (previousZone == record.timestamp.zone) return null

        val diffSeconds = record.timestamp.offset.totalSeconds - previousRecord.timestamp.offset.totalSeconds
        if (diffSeconds == 0) return null

        val sign = if (diffSeconds > 0) "+" else "-"
        val absSeconds = abs(diffSeconds)
        val hours = absSeconds / 3600
        val minutes = (absSeconds % 3600) / 60

        return buildString {
            append("(")
            append(sign)
            if (hours > 0) append("${hours}h")
            if (minutes > 0) append("${minutes}m")
            append(")")
        }
    }
}
