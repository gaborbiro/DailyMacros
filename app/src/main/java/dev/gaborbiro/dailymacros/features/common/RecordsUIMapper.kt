package dev.gaborbiro.dailymacros.features.common

import android.graphics.Bitmap
import android.util.Range
import dev.gaborbiro.dailymacros.data.records.domain.model.Record
import dev.gaborbiro.dailymacros.features.common.model.BaseListItem
import dev.gaborbiro.dailymacros.features.common.model.NutrientProgressItem
import dev.gaborbiro.dailymacros.features.common.model.NutrientProgressUIModel
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.store.bitmap.BitmapStore
import dev.gaborbiro.dailymacros.util.formatShort
import dev.gaborbiro.dailymacros.util.formatShortTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal class RecordsUIMapper(
    private val bitmapStore: BitmapStore,
    private val nutrientsUIMapper: NutrientsUIMapper,
) {
    fun map(records: List<Record>, thumbnail: Boolean): List<BaseListItem> {
        return records
            .groupBy { it.timestamp.toLocalDate() }
            .map { (day, records) ->
                listOf(
                    mapNutrientProgress(records, day)
                ) + records.map {
                    map(it, thumbnail)
                }
            }
            .flatten()
    }

    fun mapNutrientProgress(records: List<Record>, date: LocalDate): NutrientProgressUIModel {
        val totalCalories = records.sumOf { it.template.nutrients?.calories ?: 0 }
        val totalProtein =
            records.sumOf {
                it.template.nutrients?.protein?.toDouble() ?: 0.0
            }.toInt()
        val totalCarbs =
            records.sumOf {
                it.template.nutrients?.carbohydrates?.toDouble() ?: 0.0
            }.toInt()
        val totalSugar =
            records.sumOf {
                it.template.nutrients?.ofWhichSugar?.toDouble() ?: 0.0
            }.toInt()
        val totalFat =
            records.sumOf {
                it.template.nutrients?.fat?.toDouble() ?: 0.0
            }.toInt()
        val totalSalt =
            records.sumOf {
                it.template.nutrients?.salt?.toDouble() ?: 0.0
            }.toInt()
        return NutrientProgressUIModel(
            date = date,
            calories = NutrientProgressItem(
                title = "Calories",
                progress = totalCalories.toFloat() / 2500,
                progressLabel = nutrientsUIMapper.mapCalories(totalCalories, withLabel = false)!!,
                range = Range(.84f, .88f),
                rangeLabel = "2.1-2.2kcal",
            ),
            protein = NutrientProgressItem(
                title = "Protein",
                progress = totalProtein.toFloat() / 220,
                progressLabel = "${totalProtein}g",
                range = Range(.8095f, .9047f),
                rangeLabel = "170-190g",
            ),
            fat = NutrientProgressItem(
                title = "Fat",
                progress = totalFat.toFloat() / 66,
                progressLabel = "${totalFat}g",
                range = Range(.6818f, .9091f),
                rangeLabel = "45-60g",
            ),
            carbs = NutrientProgressItem(
                title = "Carbs",
                progress = totalCarbs.toFloat() / 220,
                progressLabel = "${totalCarbs}g",
                range = Range(.6818f, .9091f),
                rangeLabel = "150-200g",
            ),
            sugar = NutrientProgressItem(
                title = "Sugar",
                progress = totalSugar.toFloat() / 40,
                progressLabel = "${totalSugar}g",
                range = Range(.9f, 1f),
                rangeLabel = "<40g ttl., <25g",
            ),
            salt = NutrientProgressItem(
                title = "Salt",
                progress = totalSalt.toFloat() / 5f,
                progressLabel = "${totalSalt}g",
                range = Range(.9f, 1f),
                rangeLabel = "<5g (≈2g Na)",
            ),
        )
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
