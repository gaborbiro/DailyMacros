package dev.gaborbiro.dailymacros.repositories.settings

import com.google.gson.Gson
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import javax.inject.Inject

class SettingsMapper @Inject constructor(
    private val gson: Gson,
) {

    fun map(json: String): Targets {
        val type = object : com.google.gson.reflect.TypeToken<Targets>() {}.type
        return gson.fromJson(json, type)
    }

    fun map(targets: Targets): String = gson.toJson(targets)
}
