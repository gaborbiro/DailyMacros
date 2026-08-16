package dev.gaborbiro.dailymacros.repositories.settings

import com.google.gson.Gson
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import javax.inject.Inject

class SettingsMapper @Inject constructor(
    private val gson: Gson,
) {

    /**
     * Gson builds [Targets] via reflection and ignores Kotlin's non-null constraints, so a
     * persisted JSON blob missing one of the [Target] fields (corrupted prefs, older schema)
     * deserializes with that field null despite its declared non-null type. Parse into a
     * nullable-fields shape and substitute a default per-field instead of letting a null
     * [Target] escape into the rest of the app.
     */
    private data class TargetsJson(
        val calories: Target? = null,
        val protein: Target? = null,
        val salt: Target? = null,
        val fat: Target? = null,
        val carbs: Target? = null,
        val fibre: Target? = null,
        val ofWhichSaturated: Target? = null,
        val ofWhichSugar: Target? = null,
    )

    fun map(json: String): Targets {
        val type = object : com.google.gson.reflect.TypeToken<TargetsJson>() {}.type
        val parsed = runCatching { gson.fromJson<TargetsJson>(json, type) }.getOrNull()
        val default = Target(enabled = false)
        return Targets(
            calories = parsed?.calories ?: default,
            protein = parsed?.protein ?: default,
            salt = parsed?.salt ?: default,
            fat = parsed?.fat ?: default,
            carbs = parsed?.carbs ?: default,
            fibre = parsed?.fibre ?: default,
            ofWhichSaturated = parsed?.ofWhichSaturated ?: default,
            ofWhichSugar = parsed?.ofWhichSugar ?: default,
        )
    }

    fun map(targets: Targets): String = gson.toJson(targets)
}
