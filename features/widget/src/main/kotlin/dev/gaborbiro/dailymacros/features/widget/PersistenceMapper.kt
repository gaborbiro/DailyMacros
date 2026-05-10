package dev.gaborbiro.dailymacros.features.widget

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.features.widget.util.ZonedDateTimeAdapter
import java.time.ZonedDateTime

object PersistenceMapper {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
        .create()

    fun deserializeRecords(json: String?): List<Record> {
        return json
            ?.let {
                val itemType = object : TypeToken<List<Record>>() {}.type
                gson.fromJson(json, itemType)
            }
            ?: emptyList()
    }

    fun deserializeTemplates(json: String?): List<Template> {
        return json
            ?.let {
                val itemType = object : TypeToken<List<Template>>() {}.type
                gson.fromJson(json, itemType)
            }
            ?: emptyList()
    }

    fun serializeRecords(records: List<Record>): String {
        return gson.toJson(records)
    }

    fun serializeTemplates(temp: List<Template>): String {
        return gson.toJson(temp)
    }
}
