package dev.gaborbiro.dailymacros.repositories.records.domain.model

data class TemplateToSave(
    val images: List<String>,
    val name: String,
    val description: String,
    /** Set when this save creates a forked template (e.g. edit record with new images). */
    val parentTemplateId: Long? = null,
)
