package dev.gaborbiro.dailymacros.features.shared.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.InsertRecordsResponse
import androidx.health.connect.client.response.ReadRecordsResponse
import java.lang.reflect.Proxy
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KClass

/** An implementation of [T] whose every member fails the test, for delegating the members a fake doesn't need. */
internal inline fun <reified T : Any> unimplemented(): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        throw UnsupportedOperationException("${T::class.simpleName}.${method.name} isn't faked")
    } as T

internal class FakeHealthConnectClientProvider(
    private val sdkStatus: Int = HealthConnectClient.SDK_AVAILABLE,
    private val client: HealthConnectClient = FakeHealthConnectClient(),
) : HealthConnectClientProvider {

    var clientRequested = false
        private set

    override fun getSdkStatus(): Int = sdkStatus

    override fun getClient(): HealthConnectClient {
        clientRequested = true
        return client
    }

    override val ownPackageName: String = OWN_PACKAGE_NAME

    companion object {
        const val OWN_PACKAGE_NAME = "dev.gaborbiro.dailymacros.test"
    }
}

internal class FakeHealthConnectClient(
    grantedPermissions: Set<String> = HealthConnectClientProvider.PERMISSIONS,
    private val insertFailure: Exception? = null,
    /** Client record ids of entries this app already wrote; read back one per page. */
    private val syncedClientRecordIds: List<String> = emptyList(),
) : HealthConnectClient by unimplemented() {

    val insertedRecords = mutableListOf<Record>()
    val deletedClientRecordIds = mutableListOf<String>()

    override val permissionController: PermissionController = FakePermissionController(grantedPermissions)

    override suspend fun insertRecords(records: List<Record>): InsertRecordsResponse {
        insertFailure?.let { throw it }
        insertedRecords += records
        return InsertRecordsResponse(records.map { it.metadata.id })
    }

    override suspend fun deleteRecords(
        recordType: KClass<out Record>,
        recordIdsList: List<String>,
        clientRecordIdsList: List<String>,
    ) {
        deletedClientRecordIds += clientRecordIdsList
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Record> readRecords(request: ReadRecordsRequest<T>): ReadRecordsResponse<T> {
        check(request.recordType == NutritionRecord::class) { "Unexpected record type ${request.recordType}" }
        check(request.dataOriginFilter == setOf(DataOrigin(FakeHealthConnectClientProvider.OWN_PACKAGE_NAME))) {
            "Reading without the own data origin filter needs the read permission"
        }
        if (syncedClientRecordIds.isEmpty()) return ReadRecordsResponse(emptyList(), pageToken = null)
        val page = request.pageToken?.toInt() ?: 0
        val nextPage = (page + 1).takeIf { it < syncedClientRecordIds.size }
        return ReadRecordsResponse(
            records = listOf(syncedNutritionRecord(syncedClientRecordIds[page]) as T),
            pageToken = nextPage?.toString(),
        )
    }

    private fun syncedNutritionRecord(clientRecordId: String) = NutritionRecord(
        startTime = Instant.EPOCH,
        startZoneOffset = ZoneOffset.UTC,
        endTime = Instant.EPOCH.plusSeconds(60),
        endZoneOffset = ZoneOffset.UTC,
        metadata = Metadata.manualEntry(clientRecordId = clientRecordId, clientRecordVersion = 1L),
    )
}

private class FakePermissionController(
    private val grantedPermissions: Set<String>,
) : PermissionController by unimplemented() {

    override suspend fun getGrantedPermissions(): Set<String> = grantedPermissions
}
