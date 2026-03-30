package dev.gaborbiro.dailymacros.features.widget.util

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.ZonedDateTime

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
