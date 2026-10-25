package dev.gaborbiro.dailymacros.features.shared.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.response.InsertRecordsResponse
import java.lang.reflect.Proxy
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
}

internal class FakeHealthConnectClient(
    grantedPermissions: Set<String> = HealthConnectClientProvider.PERMISSIONS,
    private val insertFailure: Exception? = null,
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
}

private class FakePermissionController(
    private val grantedPermissions: Set<String>,
) : PermissionController by unimplemented() {

    override suspend fun getGrantedPermissions(): Set<String> = grantedPermissions
}
