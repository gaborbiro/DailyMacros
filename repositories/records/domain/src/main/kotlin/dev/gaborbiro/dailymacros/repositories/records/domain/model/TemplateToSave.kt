package dev.gaborbiro.dailymacros.repositories.records.domain.model

data class TemplateToSave(
    val imageFilenames: List<String>,
    val name: String,
    val description: String,
    /** Set when this save creates a forked template (e.g. edit record with new images). */
    val parentTemplateId: Long? = null,
    /**
     * MediaStore ids of the gallery photos the images were created from, keyed by image filename.
     * Images with no entry (in-app camera shots, shares from other apps) are saved without a
     * source id.
     */
    val imageSourceMediaStoreIds: Map<String, Long> = emptyMap(),
)
