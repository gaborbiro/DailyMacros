package dev.gaborbiro.dailymacros.features.modal.model

import dev.gaborbiro.dailymacros.features.shared.diaryDayStartTime
import dev.gaborbiro.dailymacros.features.shared.logicalDiaryDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class MealVariantListRow(
    val templateId: Long,
    val title: String,
    val lastUsed: ZonedDateTime?,
    val isCurrent: Boolean,
)

data class MealVariantListResult(
    val current: MealVariantListRow,
    val others: List<MealVariantListRow>,
) {
    val hasOtherVariants: Boolean get() = others.isNotEmpty()
}

/** Date only (no time of day), aligned with food diary day headings. */
fun formatMealVariantPickerDateOnly(time: ZonedDateTime, diaryDayStartHour: Int): String {
    val dayStart = diaryDayStartTime(diaryDayStartHour)
    return time.logicalDiaryDate(dayStart).format(DateTimeFormatter.ofPattern("E, dd MMM"))
}

data class MealVariantPickerOption(
    val templateId: Long,
    val title: String,
    val lastUsedDateLabel: String,
    val isCurrentVariant: Boolean,
)

/** Baseline for detecting unsaved edits in record details (title, description, images). */
data class RecordDetailsPristineSnapshot(
    val templateDbId: Long,
    val title: String,
    val description: String,
    val images: List<String>,
)

fun MealVariantListResult.toPickerOptions(diaryDayStartHour: Int): List<MealVariantPickerOption> {
    fun opt(row: MealVariantListRow): MealVariantPickerOption {
        val dateLabel = row.lastUsed?.let { formatMealVariantPickerDateOnly(it, diaryDayStartHour) } ?: ""
        return MealVariantPickerOption(
            templateId = row.templateId,
            title = row.title,
            lastUsedDateLabel = dateLabel,
            isCurrentVariant = row.isCurrent,
        )
    }
    return listOf(opt(current)) + others.map { opt(it) }
}
