package dev.gaborbiro.dailymacros.data.chatgpt.service.model

import com.google.gson.annotations.SerializedName

sealed class InputContent(
    @SerializedName("type") open val type: String,
) {
    data class Text(
        @SerializedName("text") val text: String,
    ) : InputContent("input_text")

    data class Image(
        @SerializedName("image_url") val base64Image: String,
    ) : InputContent("input_image")
}
