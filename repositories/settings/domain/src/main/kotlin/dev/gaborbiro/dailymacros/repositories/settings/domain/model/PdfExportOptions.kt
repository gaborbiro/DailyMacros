package dev.gaborbiro.dailymacros.repositories.settings.domain.model

/**
 * User-chosen content options for the PDF food-diary export. Persisted so the last choice is
 * remembered across exports. The date range is chosen fresh each time and is not part of this.
 */
data class PdfExportOptions(
    val dailyTotals: Boolean = true,
    val photos: PdfPhotoMode = PdfPhotoMode.TITULAR,
    val mealMacros: Boolean = true,
    val text: PdfTextMode = PdfTextMode.TITLE_AND_DESCRIPTION,
)

enum class PdfPhotoMode {
    /** Every photo attached to the meal. */
    ALL,

    /** Only the representative photo (falls back to the first one). */
    TITULAR,

    /** No photos. */
    NONE,
}

enum class PdfTextMode {
    /** Meal title only. */
    TITLE_ONLY,

    /** Meal title plus description and AI-parsed components. */
    TITLE_AND_DESCRIPTION,
}
