package dev.gaborbiro.dailymacros.repo.chatgpt.service.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type


sealed class OutputContent(
    @SerializedName("type") open val type: String,
) {
    data class Text(
        @SerializedName("text") val text: String,
    ) : OutputContent("output_text")
}

// Polymorphic deserializer for OutputContent based on "type"
internal class OutputContentDeserializer : JsonDeserializer<OutputContent> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): OutputContent {
        val obj = json.asJsonObject
        val typeStr = obj.get("type")?.asString
            ?: throw JsonParseException("Missing 'type' in OutputContent")

        return when (typeStr) {
            "output_text" -> {
                val text = obj.get("text")?.asString ?: ""
                OutputContent.Text(text)
            }

            else -> throw JsonParseException("Unknown OutputContent type: $typeStr")
        }
    }
}

// Deserializer for ContentEntry<OutputContent>
internal class ContentEntryOutputContentDeserializer :
    JsonDeserializer<ContentEntry<OutputContent>> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): ContentEntry<OutputContent> {
        val obj = json.asJsonObject

        val role = context.deserialize<Role>(obj.get("role"), Role::class.java)

        val contentArray = obj.getAsJsonArray("content")
        val contents = contentArray.map { element ->
            context.deserialize<OutputContent>(element, OutputContent::class.java)
        }

        return ContentEntry(role, contents)
    }
}
