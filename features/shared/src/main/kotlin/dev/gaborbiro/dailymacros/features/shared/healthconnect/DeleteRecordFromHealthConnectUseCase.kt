package dev.gaborbiro.dailymacros.features.shared.healthconnect

import androidx.health.connect.client.records.NutritionRecord
import javax.inject.Inject

class DeleteRecordFromHealthConnectUseCase @Inject constructor(
    private val clientProvider: HealthConnectClientProvider,
    private val getHealthConnectStatusUseCase: GetHealthConnectStatusUseCase,
    private val hasHealthConnectPermissionUseCase: HasHealthConnectPermissionUseCase,
) {

    /**
     * Removes [recordId]'s entry from Health Connect, if it was ever synced. No-op when Health
     * Connect is unavailable or the permission was revoked: nothing could have been synced since.
     */
    suspend fun execute(recordId: Long) {
        if (getHealthConnectStatusUseCase.execute() != HealthConnectStatus.AVAILABLE) return
        if (!hasHealthConnectPermissionUseCase.execute()) return
        clientProvider.getClient().deleteRecords(
            recordType = NutritionRecord::class,
            recordIdsList = emptyList(),
            clientRecordIdsList = listOf(HealthConnectClientProvider.clientRecordId(recordId)),
        )
    }
}
