package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.data.records.domain.model.Record
import dev.gaborbiro.dailymacros.features.common.mapCalories
import dev.gaborbiro.dailymacros.features.common.mapCarbohydrates
import dev.gaborbiro.dailymacros.features.common.mapFat
import dev.gaborbiro.dailymacros.features.common.mapProtein
import dev.gaborbiro.dailymacros.features.common.mapSalt
import dev.gaborbiro.dailymacros.features.common.mapSaturated
import dev.gaborbiro.dailymacros.features.common.mapSugar
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
            val caloriesGoals = "(1900-2000)"
            val proteinGoals = "(170-185)"
            val carbsGoals = "(120-150)"
            val sugarGoals = "(<50 ttl., <25 add.)"
            val fatGoals = "(50-65)"
            val saltGoals = "(<5g)"

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

                    val macros = "${mapCalories(totalCalories)} ${caloriesGoals}\n" +
                            "${mapProtein(totalProtein)} ${proteinGoals}\n" +
                            "${mapCarbohydrates(totalCarbs)} ${carbsGoals}\n" +
                            "--- ${mapSugar(totalSugar)} ${sugarGoals}\n" +
                            "${mapFat(totalFat)} ${fatGoals}\n" +
                            "--- ${mapSaturated(totalSaturated)}\n" +
                            "${mapSalt(totalSalt)} $saltGoals"

                    appContext.setMacrosPermaNotification(macros)
                }
        }
    }
}
