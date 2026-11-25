package dev.gaborbiro.dailymacros.data.db.model

/** One photo as used by one meal (template): see [dev.gaborbiro.dailymacros.data.db.TemplatesDAO.getPhotoUsages]. */
data class PhotoUsageRow(
    val filename: String,
    val mealTitle: String,
    val mealDescription: String,
    val isRepresentativeMealPhoto: Boolean?,
    /** When the meal was last logged; the meal's last update when it has no logs. */
    val lastUsedEpochMillis: Long,
)
