package dev.gaborbiro.dailymacros.features.shared.healthconnect

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import javax.inject.Inject

/**
 * Seam over Health Connect's static entry points, so the Health Connect use cases can be unit
 * tested without the SDK's provider app.
 */
interface HealthConnectClientProvider {

    /** One of `HealthConnectClient.SDK_*`. */
    fun getSdkStatus(): Int

    /** Throws when [getSdkStatus] isn't `SDK_AVAILABLE`. */
    fun getClient(): HealthConnectClient

    companion object {
        val PERMISSIONS: Set<String> = setOf(HealthPermission.getWritePermission(NutritionRecord::class))

        /** Health Connect's Play Store package; the SDK's own constant for it is internal. */
        const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"

        fun clientRecordId(recordId: Long) = "dailymacros-record-$recordId"
    }
}

class HealthConnectClientProviderImpl @Inject constructor(
    private val application: Application,
) : HealthConnectClientProvider {

    override fun getSdkStatus(): Int = HealthConnectClient.getSdkStatus(application)

    override fun getClient(): HealthConnectClient = HealthConnectClient.getOrCreate(application)
}
