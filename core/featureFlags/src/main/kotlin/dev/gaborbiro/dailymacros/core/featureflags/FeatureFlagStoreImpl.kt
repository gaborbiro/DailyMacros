package dev.gaborbiro.dailymacros.core.featureflags

import android.util.Log
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.customSignals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlagStoreImpl @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val clientIdProvider: RemoteConfigClientIdProvider,
) : FeatureFlagStore {

    // Bumped after every activation, so observers re-read their flag.
    private val activations = MutableStateFlow(0)

    init {
        val defaults = FeatureFlagStore.Key.entries.associate { it.remoteKey to it.default }
        remoteConfig.setDefaultsAsync(defaults)

        val signals = customSignals { put(CUSTOM_SIGNAL_CLIENT_ID, clientIdProvider.clientId) }
        remoteConfig.setCustomSignals(signals).addOnCompleteListener {
            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (!task.isSuccessful) Log.w(TAG, "Remote Config fetch failed", task.exception)
                onActivated()
            }
        }

        // Console changes arrive within seconds instead of after the next throttled fetch, so
        // e.g. switching Shake on for a user who just reported a problem works without a restart.
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                remoteConfig.activate().addOnCompleteListener { onActivated() }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.w(TAG, "Real-time Remote Config updates failed", error)
            }
        })
    }

    override fun isEnabled(key: FeatureFlagStore.Key): Boolean =
        remoteConfig.getBoolean(key.remoteKey)

    override fun observe(key: FeatureFlagStore.Key): Flow<Boolean> =
        activations.map { isEnabled(key) }.distinctUntilChanged()

    private fun onActivated() {
        activations.update { it + 1 }
    }

    private companion object {
        const val TAG = "FeatureFlags"

        // Matched against a Remote Config condition's custom signal in the console.
        const val CUSTOM_SIGNAL_CLIENT_ID = "client_id"
    }
}
