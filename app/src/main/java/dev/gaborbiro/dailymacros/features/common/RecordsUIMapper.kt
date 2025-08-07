package dev.gaborbiro.dailymacros.features.common

import android.graphics.Bitmap
import dev.gaborbiro.dailymacros.data.records.domain.model.Record
import dev.gaborbiro.dailymacros.features.common.model.RecordViewState
import dev.gaborbiro.dailymacros.store.bitmap.BitmapStore
import dev.gaborbiro.dailymacros.util.formatShort
import dev.gaborbiro.dailymacros.util.formatShortTime
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class RecordsUIMapper(
    private val bitmapStore: BitmapStore,
) {

    fun map(records: List<Record>, thumbnail: Boolean): List<RecordViewState> {
        return records.map {
            map(it, thumbnail)
        }
    }

    private fun map(record: Record, thumbnail: Boolean): RecordViewState {
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
        val nutrientsStr: String? = record.template.nutrients
            ?.let { it ->
                listOfNotNull(
                    it.calories?.let { "${it}kcal" },
                    it.protein?.let { "protein ${it}g" },
                    it.carbohydrates?.let { "carbs ${it}g" },
                    it.ofWhichSugar?.let { "sugar ${it}g" },
                    it.fat?.let { "fat ${it}g" },
                    it.ofWhichSaturated?.let { "saturated ${it}g" },
                    it.salt?.let { "salt ${it}g" },
                ).joinToString()
            }
            ?.takeIf { it.isNotBlank() }
        return RecordViewState(
            recordId = record.id,
            templateId = record.template.id,
            bitmap = bitmap,
            timestamp = timestampStr,
            title = record.template.name + (nutrientsStr?.let { " ($it)" } ?: ""),
        )
    }
}
