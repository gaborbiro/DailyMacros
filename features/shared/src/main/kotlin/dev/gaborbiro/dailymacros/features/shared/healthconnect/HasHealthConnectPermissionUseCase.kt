package dev.gaborbiro.dailymacros.features.shared.healthconnect

import javax.inject.Inject

/**
 * Whether the user granted every permission in [HealthConnectClientProvider.PERMISSIONS].
 * Callers must check [GetHealthConnectStatusUseCase] first: Health Connect's client can't be
 * created when it's missing or outdated, so this throws [IllegalStateException] then.
 */
class HasHealthConnectPermissionUseCase @Inject constructor(
    private val clientProvider: HealthConnectClientProvider,
    private val getHealthConnectStatusUseCase: GetHealthConnectStatusUseCase,
) {

    suspend fun execute(): Boolean {
        val status = getHealthConnectStatusUseCase.execute()
        check(status == HealthConnectStatus.AVAILABLE) {
            "Health Connect permissions checked while Health Connect is $status"
        }
        return clientProvider.getClient().permissionController
            .getGrantedPermissions()
            .containsAll(HealthConnectClientProvider.PERMISSIONS)
    }
}
