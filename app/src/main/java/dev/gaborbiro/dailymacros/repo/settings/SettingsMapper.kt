package dev.gaborbiro.dailymacros.repo.settings

import com.google.gson.Gson
import dev.gaborbiro.dailymacros.repo.settings.model.Targets

internal class SettingsMapper(
    private val gson: Gson = Gson(),
) {

    fun map(json: String): Targets {
        val type = object : com.google.gson.reflect.TypeToken<Targets>() {}.type
        return gson.fromJson(json, type)
    }

    fun map(targets: Targets): String = gson.toJson(targets)
}
