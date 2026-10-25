package dev.gaborbiro.dailymacros.features.shared.healthconnect

import androidx.health.connect.client.HealthConnectClient
import org.junit.Assert.assertEquals
import org.junit.Test

class GetHealthConnectStatusUseCaseTest {

    private fun statusFor(sdkStatus: Int) =
        GetHealthConnectStatusUseCase(FakeHealthConnectClientProvider(sdkStatus = sdkStatus)).execute()

    @Test
    fun `SDK_AVAILABLE maps to AVAILABLE`() {
        assertEquals(HealthConnectStatus.AVAILABLE, statusFor(HealthConnectClient.SDK_AVAILABLE))
    }

    @Test
    fun `provider update required maps to INSTALL_OR_UPDATE_REQUIRED`() {
        assertEquals(
            HealthConnectStatus.INSTALL_OR_UPDATE_REQUIRED,
            statusFor(HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED),
        )
    }

    @Test
    fun `SDK_UNAVAILABLE maps to NOT_SUPPORTED`() {
        assertEquals(HealthConnectStatus.NOT_SUPPORTED, statusFor(HealthConnectClient.SDK_UNAVAILABLE))
    }

    @Test(expected = IllegalStateException::class)
    fun `an unknown SDK status throws`() {
        statusFor(-1)
    }
}
