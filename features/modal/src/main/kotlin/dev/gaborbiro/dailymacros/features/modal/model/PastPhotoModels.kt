package dev.gaborbiro.dailymacros.features.modal.model

/** A photo from an earlier meal that can be added to the meal being edited. */
data class PastPhoto(
    val filename: String,
    /** The title (or, for an untitled meal, the description) of the meal that used it last. */
    val caption: String,
    /** Titles of every meal that used it, most recent first. */
    val mealTitles: List<String>,
    /** Titles and descriptions of every meal that used it, lowercased, for search. */
    val searchText: String,
    /** The AI saw a label or packaging rather than the dish: the most reusable kind of photo. */
    val isPackagingShot: Boolean,
    val lastUsedEpochMillis: Long,
)

data class PastPhotoUiModel(
    val filename: String,
    val caption: String,
)
