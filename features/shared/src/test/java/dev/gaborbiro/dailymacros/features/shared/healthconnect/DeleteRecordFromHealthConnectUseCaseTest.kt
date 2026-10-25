package dev.gaborbiro.dailymacros.features.shared.healthconnect

import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DeleteRecordFromHealthConnectUseCaseTest {

    private fun useCase(provider: HealthConnectClientProvider): DeleteRecordFromHealthConnectUseCase {
        val getStatus = GetHealthConnectStatusUseCase(provider)
        return DeleteRecordFromHealthConnectUseCase(
            clientProvider = provider,
            getHealthConnectStatusUseCase = getStatus,
            hasHealthConnectPermissionUseCase = HasHealthConnectPermissionUseCase(provider, getStatus),
        )
    }

    @Test
    fun `deletes the record's Health Connect entry by its client record id`() = runBlocking {
        val client = FakeHealthConnectClient()

        useCase(FakeHealthConnectClientProvider(client = client)).execute(recordId = 42)

        assertEquals(listOf("dailymacros-record-42"), client.deletedClientRecordIds)
    }

    @Test
    fun `no-op without creating a client when Health Connect needs installing or updating`() = runBlocking {
        val provider = FakeHealthConnectClientProvider(
            sdkStatus = HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
        )

        useCase(provider).execute(recordId = 42)

        assertFalse(provider.clientRequested)
    }

    @Test
    fun `no-op when the permission is missing`() = runBlocking {
        val client = FakeHealthConnectClient(grantedPermissions = emptySet())

        useCase(FakeHealthConnectClientProvider(client = client)).execute(recordId = 42)

        assertEquals(emptyList<String>(), client.deletedClientRecordIds)
    }
}
