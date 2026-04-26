package dev.gaborbiro.dailymacros.repositories.records.domain.model

data class TemplateToSave(
    val images: List<String>,
    /** Optional; aligned to [images] indices; missing entries default to false. */
    val coverPhotoByImageIndex: List<Boolean> = emptyList(),
    val name: String,
    val description: String,
)
