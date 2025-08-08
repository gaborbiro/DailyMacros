package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.data.records.domain.model.Record
import dev.gaborbiro.dailymacros.util.setMacrosPermaNotification
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ObserveMacrosUseCase(
    private val appContext: Context,
    private val recordsRepository: RecordsRepository,
) {

    suspend fun execute() = coroutineScope {
        launch {
            val today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
            recordsRepository
                .getRecordsFlow(today)
                .distinctUntilChanged()
                .collect { todaysRecords: List<Record> ->
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
                    val totalSaturated =
                        todaysRecords.sumOf {
                            it.template.nutrients?.ofWhichSaturated?.toDouble() ?: 0.0
                        }.toInt()
                    val totalSalt =
                        todaysRecords.sumOf {
                            it.template.nutrients?.salt?.toDouble() ?: 0.0
                        }.toInt()

                    val macros =
                        "$totalCalories cal, protein: ${totalProtein}g, carbs: ${totalCarbs}g, sugar: ${totalSugar}g, fat: ${totalFat}g, saturated: ${totalSaturated}g, salt: ${totalSalt}g"

                    appContext.setMacrosPermaNotification(macros)
                }
        }
    }
}
