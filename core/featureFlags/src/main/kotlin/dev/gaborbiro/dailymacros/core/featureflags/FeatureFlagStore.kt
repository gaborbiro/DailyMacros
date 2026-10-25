package dev.gaborbiro.dailymacros.core.featureflags

import kotlinx.coroutines.flow.Flow

interface FeatureFlagStore {

    enum class Key(val remoteKey: String, val default: Boolean) {
        AI_INSIGHTS_ENABLED("ai_insights_enabled", false),
        CUSTOMISE_AI_ENABLED("customise_ai_enabled", false),
        AUTO_PHOTO_RECOGNITION_ENABLED("auto_photo_recognition_enabled", false),

        /** On for every tester by default in the console; a client_id condition targets single users. */
        SHAKE_ENABLED("shake_enabled", false),
    }

    fun isEnabled(key: Key): Boolean

    /**
     * The current value, then every change as Remote Config activates new values - including
     * real-time updates pushed from the console mid-session.
     */
    fun observe(key: Key): Flow<Boolean>
}
