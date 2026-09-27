package dev.gaborbiro.dailymacros.features.shared.healthconnect

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

sealed class HealthConnectSyncResult {
    data class Success(val count: Int) : HealthConnectSyncResult()
    data object NotAvailable : HealthConnectSyncResult()
    data object PermissionRequired : HealthConnectSyncResult()
    data class Error(val message: String) : HealthConnectSyncResult()
}

/**
 * Pushes [Record]s to Health Connect as [NutritionRecord]s, one row per logged meal (not per meal
 * component - Health Connect has no itemized-component concept). Every write is a plain
 * `insertRecords` call keyed by a deterministic [clientRecordId] derived from [Record.recordId];
 * Health Connect itself decides insert vs. in-place replace based on that id and
 * [clientRecordVersion], so no local dedup/sync-state table is needed - see [toNutritionRecord].
 */
class HealthConnectSyncUseCase @Inject constructor(
    private val application: Application,
    private val recordsRepository: RecordsRepository,
) {

    companion object {
        val PERMISSIONS: Set<String> = setOf(HealthPermission.getWritePermission(NutritionRecord::class))

        private fun clientRecordId(recordId: Long) = "dailymacros-record-$recordId"
    }

    suspend fun hasPermission(): Boolean {
        val client = HealthConnectClient.getOrCreate(application)
        return client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
    }

    /**
     * Pushes every current [Record] logged at or after [since]. Safe to call repeatedly with an
     * overlapping range - Health Connect ignores a write whose [Metadata.clientRecordVersion]
     * isn't newer than what it already has for that [Metadata.clientRecordId].
     */
    suspend fun execute(since: ZonedDateTime): HealthConnectSyncResult {
        if (HealthConnectClient.getSdkStatus(application) != HealthConnectClient.SDK_AVAILABLE) {
            return HealthConnectSyncResult.NotAvailable
        }
        if (!hasPermission()) {
            return HealthConnectSyncResult.PermissionRequired
        }
        return try {
            val client = HealthConnectClient.getOrCreate(application)
            val nutritionRecords = recordsRepository.getRecords(since = since)
                .filter { it.template.nutrients.calories != null }
                .map { it.toNutritionRecord() }
            if (nutritionRecords.isNotEmpty()) {
                client.insertRecords(nutritionRecords)
            }
            HealthConnectSyncResult.Success(nutritionRecords.size)
        } catch (e: Exception) {
            HealthConnectSyncResult.Error(e.message ?: e.toString())
        }
    }

    /** Removes [recordId]'s entry from Health Connect, if it was ever synced. No-op otherwise. */
    suspend fun deleteRecord(recordId: Long) {
        if (HealthConnectClient.getSdkStatus(application) != HealthConnectClient.SDK_AVAILABLE) return
        if (!hasPermission()) return
        val client = HealthConnectClient.getOrCreate(application)
        client.deleteRecords(
            recordType = NutritionRecord::class,
            recordIdsList = emptyList(),
            clientRecordIdsList = listOf(clientRecordId(recordId)),
        )
    }

    /**
     * Health Connect requires startTime to be strictly before endTime (equal instants are
     * rejected with "startTime must be before endTime"), so this treats the meal as spanning one
     * minute starting at the logged time rather than a true instant.
     *
     * [Nutrients.ofWhichAddedSugar] has no Health Connect equivalent and is dropped.
     * [Nutrients.salt] is converted to sodium (salt ÷ 2.5).
     */
    private fun Record.toNutritionRecord(): NutritionRecord {
        val startInstant = timestamp.toInstant()
        val zoneOffset = timestamp.offset
        val nutrients = template.nutrients
        return NutritionRecord(
            startTime = startInstant,
            startZoneOffset = zoneOffset,
            endTime = startInstant + 1.minutes.toJavaDuration(),
            endZoneOffset = zoneOffset,
            metadata = Metadata.manualEntry(
                clientRecordId = clientRecordId(recordId),
                clientRecordVersion = System.currentTimeMillis(),
            ),
            name = template.name.ifBlank { null },
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
