package dev.gaborbiro.dailymacros.features.common

import android.graphics.Bitmap
import android.util.Range
import dev.gaborbiro.dailymacros.data.image.ImageStore
import dev.gaborbiro.dailymacros.features.common.model.BaseListItemUIModel
import dev.gaborbiro.dailymacros.features.common.model.NutrientProgressItem
import dev.gaborbiro.dailymacros.features.common.model.NutrientProgressUIModel
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.util.formatShort
import dev.gaborbiro.dailymacros.util.formatShortTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal class RecordsUIMapper(
    private val imageStore: ImageStore,
    private val nutrientsUIMapper: NutrientsUIMapper,
) {
    fun map(records: List<Record>, thumbnail: Boolean): List<BaseListItemUIModel> {
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

        class NutrientProfile(
            val name: String,
            val rangeLabel: String,
            val min: Float,
            val max: Float,
        ) {
            private val absoluteMax = max * 1.1f
            private val lowRange = min / absoluteMax
            private val highRange = max / absoluteMax
            val range = Range(lowRange, highRange)
            fun progress(total: Int) = total / absoluteMax
        }

        val calories = NutrientProfile(
            name = "Calories",
            rangeLabel = "2.1-2.2kcal",
            min = 2100f,
            max = 2200f,
        )
        val carbs = NutrientProfile(
            name = "Carbs",
            rangeLabel = "150-200g",
            min = 150f,
            max = 200f,
        )
        val protein = NutrientProfile(
            name = "Protein",
            rangeLabel = "170-190g",
            min = 170f,
            max = 190f,
        )
        val fat = NutrientProfile(
            name = "Fat",
            rangeLabel = "55-65g",
            min = 55f,
            max = 65f,
        )
        val sugar = NutrientProfile(
            name = "Sugar",
            rangeLabel = "<40g ttl., <25g",
            min = 0f,
            max = 40f,
        )
        val salt = NutrientProfile(
            name = "Salt",
            rangeLabel = "<5g (≈2g Na)",
            min = 0f,
            max = 5f,
        )

        return NutrientProgressUIModel(
            date = date,
            calories = NutrientProgressItem(
                title = calories.name,
                progress = calories.progress(totalCalories),
                progressLabel = nutrientsUIMapper.mapCalories(totalCalories, withLabel = false)!!,
                range = calories.range,
                rangeLabel = calories.rangeLabel,
            ),
            protein = NutrientProgressItem(
                title = protein.name,
                progress = protein.progress(totalProtein),
                progressLabel = "${totalProtein}g",
                range = protein.range,
                rangeLabel = protein.rangeLabel,
            ),
            fat = NutrientProgressItem(
                title = fat.name,
                progress = fat.progress(totalFat),
                progressLabel = "${totalFat}g",
                range = fat.range,
                rangeLabel = fat.rangeLabel,
            ),
            carbs = NutrientProgressItem(
                title = carbs.name,
                progress = carbs.progress(totalCarbs),
                progressLabel = "${totalCarbs}g",
                range = carbs.range,
                rangeLabel = carbs.rangeLabel,
            ),
            sugar = NutrientProgressItem(
                title = sugar.name,
                progress = totalSugar.toFloat() / 40,
                progressLabel = "${totalSugar}g",
                range = Range(.9f, 1f),
                rangeLabel = sugar.rangeLabel,
            ),
            salt = NutrientProgressItem(
                title = "Salt",
                progress = totalSalt.toFloat() / 5f,
                progressLabel = "${totalSalt}g",
                range = Range(.9f, 1f),
                rangeLabel = salt.rangeLabel,
            ),
        )
    }

    private fun map(record: Record, thumbnail: Boolean): RecordUIModel {
        val bitmap: Bitmap? = record.template.primaryImage?.let { imageStore.read(it, thumbnail) }
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
            recordId = record.dbId,
            templateId = record.template.dbId,
            bitmap = bitmap,
            timestamp = timestampStr,
            title = record.template.name,
            description = nutrientsStr ?: "",
            hasNutrients = record.template.nutrients != null,
        )
    }
}
