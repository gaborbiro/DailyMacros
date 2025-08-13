package dev.gaborbiro.dailymacros.features.overview.useCases

import android.util.Range
import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.data.records.domain.model.Record
import dev.gaborbiro.dailymacros.features.common.NutrientsUIMapper
import dev.gaborbiro.dailymacros.features.overview.model.NutrientProgressItem
import dev.gaborbiro.dailymacros.features.overview.model.NutrientProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal class ObserveMacroGoalsProgressUseCase(
    private val recordsRepository: RecordsRepository,
    private val nutrientsUIMapper: NutrientsUIMapper,
) {

    suspend fun execute(): Flow<NutrientProgress> {
        val today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        return recordsRepository
            .getRecordsFlow(today)
            .distinctUntilChanged()
            .map { todaysRecords: List<Record> ->
                val totalCalories = todaysRecords.sumOf { it.template.nutrients?.calories ?: 0 }
                val totalProtein =
                    todaysRecords.sumOf {
                        it.template.nutrients?.protein?.toDouble() ?: 0.0
                    }.toInt()
                val totalCarbs =
                    todaysRecords.sumOf {
                        it.template.nutrients?.carbohydrates?.toDouble() ?: 0.0
                    }.toInt()
                val totalSugar =
                    todaysRecords.sumOf {
                        it.template.nutrients?.ofWhichSugar?.toDouble() ?: 0.0
                    }.toInt()
                val totalFat =
                    todaysRecords.sumOf {
                        it.template.nutrients?.fat?.toDouble() ?: 0.0
                    }.toInt()
                val totalSalt =
                    todaysRecords.sumOf {
                        it.template.nutrients?.salt?.toDouble() ?: 0.0
                    }.toInt()
                NutrientProgress(
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
    }
}
