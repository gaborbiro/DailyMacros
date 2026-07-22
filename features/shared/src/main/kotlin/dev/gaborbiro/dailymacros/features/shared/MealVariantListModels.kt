package dev.gaborbiro.dailymacros.features.shared

import java.time.ZonedDateTime

data class MealVariantListRow(
    val templateId: Long,
    val title: String,
    val lastUsed: ZonedDateTime?,
    val isCurrent: Boolean,
    val componentNames: List<String> = emptyList(),
)

data class MealVariantListResult(
    val current: MealVariantListRow,
    val others: List<MealVariantListRow>,
) {
    val hasOtherVariants: Boolean get() = others.isNotEmpty()
}
