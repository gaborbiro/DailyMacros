package dev.gaborbiro.dailymacros.core.featureflags

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlagStoreImpl @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
) : FeatureFlagStore {

    init {
        val defaults = FeatureFlagStore.Key.entries.associate { it.remoteKey to it.default }
        remoteConfig.setDefaultsAsync(defaults)
        remoteConfig.fetchAndActivate()
    }

    override fun isEnabled(key: FeatureFlagStore.Key): Boolean =
        remoteConfig.getBoolean(key.remoteKey)
}
