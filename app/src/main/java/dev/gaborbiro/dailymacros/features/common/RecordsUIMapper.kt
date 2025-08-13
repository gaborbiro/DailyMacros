package dev.gaborbiro.dailymacros.features.common

import android.graphics.Bitmap
import dev.gaborbiro.dailymacros.data.records.domain.model.Record
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.store.bitmap.BitmapStore
import dev.gaborbiro.dailymacros.util.formatShort
import dev.gaborbiro.dailymacros.util.formatShortTime
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal class RecordsUIMapper(
    private val bitmapStore: BitmapStore,
    private val nutrientsUIMapper: NutrientsUIMapper,
) {
    fun map(records: List<Record>, thumbnail: Boolean): List<RecordUIModel> {
        return records.map {
            map(it, thumbnail)
        }
    }

    private fun map(record: Record, thumbnail: Boolean): RecordUIModel {
        val bitmap: Bitmap? = record.template.image?.let { bitmapStore.read(it, thumbnail) }
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
        val nutrientsStr: String? =
            record.template.nutrients?.let { nutrientsUIMapper.map(it, isShort = true) }

        return RecordUIModel(
            recordId = record.id,
            templateId = record.template.id,
            bitmap = bitmap,
            timestamp = timestampStr,
            title = record.template.name,
            description = nutrientsStr ?: "",
        )
    }
}
