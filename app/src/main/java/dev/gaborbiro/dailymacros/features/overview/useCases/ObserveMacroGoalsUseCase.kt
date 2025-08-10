package dev.gaborbiro.dailymacros.features.overview.useCases

import android.util.Range
import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.data.records.domain.model.Record
import dev.gaborbiro.dailymacros.features.common.views.GoalCellItem
import dev.gaborbiro.dailymacros.features.common.views.MacroGoalsUIModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ObserveMacroGoalsUseCase(
    private val recordsRepository: RecordsRepository,
) {

    suspend fun execute(): Flow<MacroGoalsUIModel> {
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
                MacroGoalsUIModel(
                    calories = GoalCellItem(
                        title = "Calories",
                        value = "$totalCalories cal",
                        rangeLabel = "2.1-2.2kcal",
                        range = Range(.84f, .88f),
                        progress = totalCalories.toFloat() / 2500
                    ),
                    protein = GoalCellItem(
                        title = "Protein",
                        value = "${totalProtein}g",
                        rangeLabel = "170-190g",
                        range = Range(.8095f, .9047f),
                        progress = totalProtein.toFloat() / 220
                    ),
                    fat = GoalCellItem(
                        title = "Fat",
                        value = "${totalFat}g",
                        rangeLabel = "45-60g",
                        range = Range(.6818f, .9091f),
                        progress = totalFat.toFloat() / 66
                    ),
                    carbs = GoalCellItem(
                        title = "Carbs",
                        value = "${totalCarbs}g",
                        rangeLabel = "150-200g",
                        range = Range(.6818f, .9091f),
                        progress = totalCarbs.toFloat() / 220
                    ),
                    sugar = GoalCellItem(
                        title = "Sugar",
                        value = "${totalSugar}g",
                        rangeLabel = "<40g ttl., <25g",
                        range = Range(.9f, 1f),
                        progress = totalSugar.toFloat() / 40
                    ),
                    salt = GoalCellItem(
                        title = "Salt",
                        value = "${totalSalt}g",
                        rangeLabel = "<5g (≈2g Na)",
                        range = Range(.9f, 1f),
                        progress = totalSalt.toFloat() / 5f
                    ),
                )
            }
    }
}
