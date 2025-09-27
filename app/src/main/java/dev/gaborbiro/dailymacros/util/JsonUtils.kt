package dev.gaborbiro.dailymacros.util

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.ZonedDateTime

val gson: Gson = GsonBuilder()
    .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
    .registerTypeAdapter(Uri::class.java, UriAdapter())
    .create()

class ZonedDateTimeAdapter : JsonDeserializer<ZonedDateTime>, JsonSerializer<ZonedDateTime> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): ZonedDateTime {
        return ZonedDateTime.parse(json.asString)
    }

    override fun serialize(
        src: ZonedDateTime,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement {
        return JsonPrimitive(src.toString())
        // produces e.g. "2025-09-27T13:45:00+01:00[Europe/Lisbon]"
    }
}

class UriAdapter : JsonDeserializer<Uri?>, JsonSerializer<Uri?> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): Uri? {
        return when (json) {
            is JsonNull -> null
            else -> Uri.parse(json.asString)
        }
    }

    override fun serialize(
        src: Uri?,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement {
        return src?.let { JsonPrimitive(src.toString()) } ?: JsonNull.INSTANCE
    }
}
