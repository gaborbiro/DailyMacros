package dev.gaborbiro.nutri.data.chatgpt.model

data class NutrientsRequest(
    val base64Image: String?,
    val title: String,
    val description: String,
)
