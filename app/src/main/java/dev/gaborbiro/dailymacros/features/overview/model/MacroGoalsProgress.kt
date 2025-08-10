package dev.gaborbiro.dailymacros.features.overview.model

import android.util.Range

data class MacroGoalsProgress(
    val calories: GoalCellItem,
    val protein: GoalCellItem,
    val fat: GoalCellItem,
    val carbs: GoalCellItem,
    val sugar: GoalCellItem,
    val salt: GoalCellItem,
)

data class GoalCellItem(
    val title: String,
    val value: String,
    val rangeLabel: String,
    val range: Range<Float>,
    val progress: Float,
)
