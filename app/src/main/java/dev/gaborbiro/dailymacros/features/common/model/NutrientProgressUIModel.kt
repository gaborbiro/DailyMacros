package dev.gaborbiro.dailymacros.features.common.model

import android.util.Range
import java.time.LocalDate

data class NutrientProgressUIModel(
    val date: LocalDate,
    val calories: NutrientProgressItem,
    val protein: NutrientProgressItem,
    val fat: NutrientProgressItem,
    val carbs: NutrientProgressItem,
    val sugar: NutrientProgressItem,
    val salt: NutrientProgressItem,
): BaseListItem {
    override val id = date
}

data class NutrientProgressItem(
    val title: String,
    val progress: Float,
    val progressLabel: String,
    val range: Range<Float>,
    val rangeLabel: String,
)
