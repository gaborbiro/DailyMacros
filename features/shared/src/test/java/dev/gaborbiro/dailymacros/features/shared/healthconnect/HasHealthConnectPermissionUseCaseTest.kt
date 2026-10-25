package dev.gaborbiro.dailymacros.features.shared.healthconnect

import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HasHealthConnectPermissionUseCaseTest {

    private fun useCase(provider: HealthConnectClientProvider) =
        HasHealthConnectPermissionUseCase(provider, GetHealthConnectStatusUseCase(provider))

    @Test
    fun `true when every permission is granted`() = runBlocking {
        val provider = FakeHealthConnectClientProvider()

        assertTrue(useCase(provider).execute())
    }

    @Test
    fun `false when a permission is missing`() = runBlocking {
        val provider = FakeHealthConnectClientProvider(
            client = FakeHealthConnectClient(grantedPermissions = emptySet()),
        )

        assertFalse(useCase(provider).execute())
    }

    @Test
    fun `throws without creating a client when Health Connect needs installing or updating`() {
        val provider = FakeHealthConnectClientProvider(
            sdkStatus = HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
        )

        assertThrows(IllegalStateException::class.java) {
            runBlocking { useCase(provider).execute() }
        }
        assertFalse(provider.clientRequested)
    }
}
