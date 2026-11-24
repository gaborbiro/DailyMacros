package dev.gaborbiro.dailymacros.repositories.records.domain.model

/** One photo as used by one meal. A photo shared by several meals has one [PhotoUsage] per meal. */
data class PhotoUsage(
    val filename: String,
    val mealTitle: String,
    val mealDescription: String,
    /** From nutrient analysis: false for packaging/label shots; null when never classified. */
    val isRepresentativeOfMeal: Boolean?,
    /** When the meal was last logged; the meal's last update when it has no logs. */
    val lastUsedEpochMillis: Long,
)
