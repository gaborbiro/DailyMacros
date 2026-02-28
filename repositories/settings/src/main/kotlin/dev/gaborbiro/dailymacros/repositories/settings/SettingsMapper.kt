package dev.gaborbiro.dailymacros.repositories.settings

import com.google.gson.Gson
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets

class SettingsMapper(
    private val gson: Gson = Gson(),
) {

    fun map(json: String): Targets {
        val type = object : com.google.gson.reflect.TypeToken<Targets>() {}.type
        return gson.fromJson(json, type)
    }

    fun map(targets: Targets): String = gson.toJson(targets)
}
