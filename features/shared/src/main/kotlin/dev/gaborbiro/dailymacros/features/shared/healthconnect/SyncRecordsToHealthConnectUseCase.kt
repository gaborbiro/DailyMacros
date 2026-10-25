package dev.gaborbiro.dailymacros.features.shared.healthconnect

import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import dev.gaborbiro.dailymacros.features.common.utils.diaryDayStartTime
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.common.model.toZonedDateTime
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

sealed class HealthConnectSyncResult {
    data class Success(val count: Int) : HealthConnectSyncResult()
    data object NotAvailable : HealthConnectSyncResult()
    data object PermissionRequired : HealthConnectSyncResult()
    data class Error(val message: String) : HealthConnectSyncResult()
}

/**
 * Pushes [Record]s to Health Connect as [NutritionRecord]s, one row per logged meal (not per meal
 * component - Health Connect has no itemized-component concept). Every write is a plain
 * `insertRecords` call keyed by a deterministic [HealthConnectClientProvider.clientRecordId]
 * derived from [Record.recordId]; Health Connect itself decides insert vs. in-place replace based
 * on that id and [Metadata.clientRecordVersion], so no local dedup/sync-state table is needed -
 * see [toNutritionRecord].
 */
class SyncRecordsToHealthConnectUseCase @Inject constructor(
    private val clientProvider: HealthConnectClientProvider,
    private val recordsRepository: RecordsRepository,
    private val settingsRepository: SettingsRepository,
    private val getHealthConnectStatusUseCase: GetHealthConnectStatusUseCase,
    private val hasHealthConnectPermissionUseCase: HasHealthConnectPermissionUseCase,
) {

    /**
     * Pushes every current [Record] logged at or after [since]. Safe to call repeatedly with an
     * overlapping range - Health Connect ignores a write whose [Metadata.clientRecordVersion]
     * isn't newer than what it already has for that [Metadata.clientRecordId].
     */
    suspend fun execute(since: ZonedDateTime): HealthConnectSyncResult {
        if (getHealthConnectStatusUseCase.execute() != HealthConnectStatus.AVAILABLE) {
            return HealthConnectSyncResult.NotAvailable
        }
        if (!hasHealthConnectPermissionUseCase.execute()) {
            return HealthConnectSyncResult.PermissionRequired
        }
        return try {
            val client = clientProvider.getClient()
            val dayStart = diaryDayStartTime(settingsRepository.getDiaryDayStartHour())
            val nutritionRecords = recordsRepository.getRecords(since = since.toInstant().toKotlinInstant())
                .filter { it.template.nutrients.calories != null }
                .map { it.toNutritionRecord(dayStart) }
            if (nutritionRecords.isNotEmpty()) {
                client.insertRecords(nutritionRecords)
            }
            HealthConnectSyncResult.Success(nutritionRecords.size)
        } catch (e: Exception) {
            HealthConnectSyncResult.Error(e.message ?: e.toString())
        }
    }

    /**
     * Health Connect requires startTime to be strictly before endTime (equal instants are
     * rejected with "startTime must be before endTime"), so this treats the meal as spanning one
     * minute starting at the logged time rather than a true instant.
     *
     * [Nutrients.ofWhichAddedSugar] has no Health Connect equivalent and is dropped.
     * [Nutrients.salt] is converted to sodium (salt ÷ 2.5).
     * The meal type comes from the logged time, see [healthConnectMealType].
     */
    private fun Record.toNutritionRecord(dayStart: LocalTime): NutritionRecord {
        val startInstant = timestamp.instant.toJavaInstant()
        val zonedTimestamp = timestamp.toZonedDateTime()
        val zoneOffset = zonedTimestamp.offset
        val nutrients = template.nutrients
        return NutritionRecord(
            startTime = startInstant,
            startZoneOffset = zoneOffset,
            endTime = startInstant + 1.minutes.toJavaDuration(),
            endZoneOffset = zoneOffset,
            metadata = Metadata.manualEntry(
                clientRecordId = HealthConnectClientProvider.clientRecordId(recordId),
                clientRecordVersion = System.currentTimeMillis(),
            ),
            name = template.name.ifBlank { null },
            mealType = healthConnectMealType(zonedTimestamp, dayStart),
            energy = nutrients.calories?.let { Energy.kilocalories(it.toDouble()) },
            protein = nutrients.protein?.let { Mass.grams(it.toDouble()) },
            totalFat = nutrients.fat?.let { Mass.grams(it.toDouble()) },
            saturatedFat = nutrients.ofWhichSaturated?.let { Mass.grams(it.toDouble()) },
            totalCarbohydrate = nutrients.carbs?.let { Mass.grams(it.toDouble()) },
            sugar = nutrients.ofWhichSugar?.let { Mass.grams(it.toDouble()) },
            dietaryFiber = nutrients.fibre?.let { Mass.grams(it.toDouble()) },
            sodium = nutrients.salt?.let { Mass.grams(it / 2.5) },
        )
    }
}
