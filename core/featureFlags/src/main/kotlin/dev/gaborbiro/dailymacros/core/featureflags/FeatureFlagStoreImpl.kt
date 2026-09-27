package dev.gaborbiro.dailymacros.core.featureflags

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.customSignals
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlagStoreImpl @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val clientIdProvider: RemoteConfigClientIdProvider,
) : FeatureFlagStore {

    init {
        val defaults = FeatureFlagStore.Key.entries.associate { it.remoteKey to it.default }
        remoteConfig.setDefaultsAsync(defaults)

        val signals = customSignals { put(CUSTOM_SIGNAL_CLIENT_ID, clientIdProvider.clientId) }
        remoteConfig.setCustomSignals(signals).addOnCompleteListener {
            remoteConfig.fetchAndActivate()
        }
    }

    override fun isEnabled(key: FeatureFlagStore.Key): Boolean =
        remoteConfig.getBoolean(key.remoteKey)

    private companion object {
        // Matched against a Remote Config condition's custom signal in the console.
        const val CUSTOM_SIGNAL_CLIENT_ID = "client_id"
    }
}
