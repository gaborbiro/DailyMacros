package dev.gaborbiro.dailymacros.repositories.settings.domain.model

/**
 * User-chosen content options for the PDF food-diary export. Persisted so the last choice is
 * remembered across exports. The date range is chosen fresh each time and is not part of this.
 * The meal title (time + name) is always included.
 */
data class PdfExportOptions(
    val dailyTotals: Boolean = true,
    val photos: PdfPhotoMode = PdfPhotoMode.TITULAR,
    val mealMacros: Boolean = true,
    val description: Boolean = true,
    val components: Boolean = true,
)

enum class PdfPhotoMode {
    /** Every photo attached to the meal. */
    ALL,

    /** Only the representative photo (falls back to the first one). */
    TITULAR,

    /** No photos. */
    NONE,
}
