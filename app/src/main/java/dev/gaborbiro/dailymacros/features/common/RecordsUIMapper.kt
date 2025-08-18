package dev.gaborbiro.dailymacros.features.common

import android.util.Range
import dev.gaborbiro.dailymacros.features.common.model.BaseListItemUIModel
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressUIModel
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.util.formatShort
import dev.gaborbiro.dailymacros.util.formatShortTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal class RecordsUIMapper(
    private val macrosUIMapper: MacrosUIMapper,
) {
    fun map(records: List<Record>): List<BaseListItemUIModel> {
        return records
            .groupBy { it.timestamp.toLocalDate() }
            .map { (day, records) ->
                listOf(
                    mapMacrosProgress(records, day)
                ) + records.map {
                    map(it)
                }
            }
            .flatten()
    }

    fun mapMacrosProgress(records: List<Record>, date: LocalDate): MacroProgressUIModel {
        val totalCalories = records.sumOf { it.template.macros?.calories ?: 0 }
        val totalProtein = records.sumOf { it.template.macros?.protein?.toDouble() ?: 0.0 }
            .toInt()
        val totalCarbs = records.sumOf { it.template.macros?.carbohydrates?.toDouble() ?: 0.0 }
            .toInt()
        val totalSugar = records.sumOf { it.template.macros?.ofWhichSugar?.toDouble() ?: 0.0 }
            .toInt()
        val totalFat = records.sumOf { it.template.macros?.fat?.toDouble() ?: 0.0 }
            .toInt()
        val totalSalt = records.sumOf { it.template.macros?.salt?.toDouble() ?: 0.0 }
            .toInt()
        val totalFibre = records.sumOf { it.template.macros?.fibre?.toDouble() ?: 0.0 }
            .toInt()

        class MacroGoal(
            val name: String,
            val rangeLabel: String,
            val min: Float,
            val max: Float,
            val fuzzy: Boolean,
        ) {
            private val extendedMax = max * 1.1f
            val targetRange = Range(
                min / (if (fuzzy) extendedMax else max),
                max / (if (fuzzy) extendedMax else 1f)
            )

            fun progress(total: Int) = total / if (fuzzy) extendedMax else max
        }

        val calories = MacroGoal(
            name = "Calories",
            rangeLabel = "2.1-2.2kcal",
            min = 2100f,
            max = 2200f,
            fuzzy = true,
        )
        val protein = MacroGoal(
            name = "Protein",
            rangeLabel = "170-190g",
            min = 170f,
            max = 190f,
            fuzzy = true,
        )
        val fat = MacroGoal(
            name = "Fat",
            rangeLabel = "55-65g",
            min = 55f,
            max = 65f,
            fuzzy = true,
        )
        val carbs = MacroGoal(
            name = "Carbs",
            rangeLabel = "150-200g",
            min = 150f,
            max = 200f,
            fuzzy = true,
        )
        val sugar = MacroGoal(
            name = "Sugar",
            rangeLabel = "<40g ttl., <25g3",
            min = 0f,
            max = 40f,
            fuzzy = false,
        )
        val salt = MacroGoal(
            name = "Salt",
            rangeLabel = "<5g (≈2g Na)",
            min = 0f,
            max = 5f,
            fuzzy = false,
        )
        val fibre = MacroGoal(
            // TODO Women need 21-25g
            name = "Fibre",
            rangeLabel = "30-38g",
            min = 30f,
            max = 38f,
            fuzzy = true,
        )

        val macros = listOf(
            MacroProgressItem(
                title = calories.name,
                progress = calories.progress(totalCalories),
                progressLabel = macrosUIMapper.mapCalories(totalCalories, withLabel = false)!!,
                range = calories.targetRange,
                rangeLabel = calories.rangeLabel,
            ),
            MacroProgressItem(
                title = protein.name,
                progress = protein.progress(totalProtein),
                progressLabel = macrosUIMapper.mapProtein(totalCarbs, withLabel = false) ?: "0g",
                range = protein.targetRange,
                rangeLabel = protein.rangeLabel,
            ),
            MacroProgressItem(
                title = fat.name,
                progress = fat.progress(totalFat),
                progressLabel = macrosUIMapper.mapFat(totalFat, null, withLabel = false) ?: "0g",
                range = fat.targetRange,
                rangeLabel = fat.rangeLabel,
            ),
            MacroProgressItem(
                title = carbs.name,
                progress = carbs.progress(totalCarbs),
                progressLabel = macrosUIMapper.mapCarbohydrates(totalCarbs, null, withLabel = false)
                    ?: "0g",
                range = carbs.targetRange,
                rangeLabel = carbs.rangeLabel,
            ),
            MacroProgressItem(
                title = sugar.name,
                progress = sugar.progress(totalSugar),
                progressLabel = macrosUIMapper.mapProtein(totalSugar, withLabel = false) ?: "0g",
                range = sugar.targetRange,
                rangeLabel = sugar.rangeLabel,
            ),
            MacroProgressItem(
                title = salt.name,
                progress = salt.progress(totalSalt),
                progressLabel = macrosUIMapper.mapProtein(totalSalt, withLabel = false) ?: "0g",
                range = salt.targetRange,
                rangeLabel = salt.rangeLabel,
            ),
            MacroProgressItem(
                title = fibre.name,
                progress = fibre.progress(totalFibre),
                progressLabel = macrosUIMapper.mapProtein(totalFibre, withLabel = false) ?: "0g",
                range = fibre.targetRange,
                rangeLabel = fibre.rangeLabel,
            ),
        )

        return MacroProgressUIModel(
            date = date,
            macros = macros,
        )
    }

    private fun map(record: Record): RecordUIModel {
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
        val macrosStr: String? =
            record.template.macros?.let { macrosUIMapper.map(it, isShort = true) }

        return RecordUIModel(
            recordId = record.dbId,
            templateId = record.template.dbId,
            images = record.template.images,
            timestamp = timestampStr,
            title = record.template.name,
            description = macrosStr ?: "",
            hasMacros = record.template.macros != null,
        )
    }
}
