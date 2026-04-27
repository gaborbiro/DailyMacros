package dev.gaborbiro.dailymacros.repositories.records.domain.model

data class TemplateToSave(
    val images: List<String>,
    /** Optional; aligned to [images]; null = unknown; omit or use empty for all-null in DB. */
    val coverPhotoByImageIndex: List<Boolean?> = emptyList(),
    val name: String,
    val description: String,
)
