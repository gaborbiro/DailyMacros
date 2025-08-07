package dev.gaborbiro.dailymacros.util

import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val gson = GsonBuilder()
    .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
    .registerTypeAdapter(Uri::class.java, UriAdapter())
    .create()

class LocalDateTimeAdapter : JsonDeserializer<LocalDateTime>, JsonSerializer<LocalDateTime> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LocalDateTime {
        return LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    override fun serialize(
        src: LocalDateTime,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }
}

class UriAdapter : JsonDeserializer<Uri?>, JsonSerializer<Uri?> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Uri? {
        return when(json) {
            is JsonNull -> null
            else -> Uri.parse(json.asString)
        }
    }

    override fun serialize(
        src: Uri?,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return src?.let { JsonPrimitive(src.toString()) } ?: JsonNull.INSTANCE
    }
}
