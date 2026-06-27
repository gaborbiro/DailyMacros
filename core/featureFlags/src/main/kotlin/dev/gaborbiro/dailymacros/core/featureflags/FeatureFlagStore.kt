package dev.gaborbiro.dailymacros.core.featureflags

interface FeatureFlagStore {

    enum class Key(val remoteKey: String, val default: Boolean) {
        AI_INSIGHTS_ENABLED("ai_insights_enabled", false),
        CUSTOMISE_AI_ENABLED("customise_ai_enabled", false),
    }

    fun isEnabled(key: Key): Boolean
}
