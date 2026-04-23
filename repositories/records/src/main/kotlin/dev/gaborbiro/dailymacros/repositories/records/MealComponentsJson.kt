package dev.gaborbiro.dailymacros.repositories.records

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.dailymacros.repositories.records.domain.model.ComponentConfidence
import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent

private val gson = Gson()

internal data class MealComponentDto(
    @SerializedName("name") val name: String,
    @SerializedName("estimatedAmount") val estimatedAmount: String,
    @SerializedName("confidence") val confidence: String,
)

internal fun decodeMealComponentsJson(json: String?): List<MealComponent> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = gson.fromJson(json, Array<MealComponentDto>::class.java) ?: emptyArray()
        array.map { it.toDomain() }
    }.getOrElse { emptyList() }
}

internal fun encodeMealComponentsJson(components: List<MealComponent>): String? {
    if (components.isEmpty()) return null
    val dtos = components.map { MealComponentDto(it.name, it.estimatedAmount, it.confidence.toApiString()) }
    return gson.toJson(dtos)
}

internal fun MealComponentDto.toDomain(): MealComponent {
    return MealComponent(
        name = name,
        estimatedAmount = estimatedAmount,
        confidence = when (confidence.lowercase()) {
            "medium" -> ComponentConfidence.MEDIUM
            "low" -> ComponentConfidence.LOW
            else -> ComponentConfidence.HIGH
        },
    )
}

private fun ComponentConfidence.toApiString(): String = when (this) {
    ComponentConfidence.HIGH -> "high"
    ComponentConfidence.MEDIUM -> "medium"
    ComponentConfidence.LOW -> "low"
}
