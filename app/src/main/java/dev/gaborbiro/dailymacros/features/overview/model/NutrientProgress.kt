package dev.gaborbiro.dailymacros.features.overview.model

import android.util.Range

data class NutrientProgress(
    val calories: NutrientProgressItem,
    val protein: NutrientProgressItem,
    val fat: NutrientProgressItem,
    val carbs: NutrientProgressItem,
    val sugar: NutrientProgressItem,
    val salt: NutrientProgressItem,
)

data class NutrientProgressItem(
    val title: String,
    val progress: Float,
    val progressLabel: String,
    val range: Range<Float>,
    val rangeLabel: String,
)
