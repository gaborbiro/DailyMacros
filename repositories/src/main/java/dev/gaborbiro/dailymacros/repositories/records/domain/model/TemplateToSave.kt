package dev.gaborbiro.dailymacros.repositories.records.domain.model

data class TemplateToSave(
    val images: List<String>,
    val name: String,
    val description: String,
)
