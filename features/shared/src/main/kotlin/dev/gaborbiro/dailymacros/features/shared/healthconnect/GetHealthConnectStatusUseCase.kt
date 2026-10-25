package dev.gaborbiro.dailymacros.features.shared.healthconnect

import androidx.health.connect.client.HealthConnectClient
import javax.inject.Inject

/**
 * On Android 12-13 Health Connect is a separate Play Store app that may be missing or outdated;
 * from Android 14 it's part of the platform. Check this before [HasHealthConnectPermissionUseCase],
 * which throws unless Health Connect is [HealthConnectStatus.AVAILABLE].
 */
class GetHealthConnectStatusUseCase @Inject constructor(
    private val clientProvider: HealthConnectClientProvider,
) {

    fun execute(): HealthConnectStatus = when (val sdkStatus = clientProvider.getSdkStatus()) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.INSTALL_OR_UPDATE_REQUIRED
        HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectStatus.NOT_SUPPORTED
        else -> throw IllegalStateException("Unknown Health Connect SDK status: $sdkStatus")
    }
}
