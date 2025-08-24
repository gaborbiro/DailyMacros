package dev.gaborbiro.dailymacros.features.common.model

import androidx.compose.runtime.Stable

@Stable
data class RecordUIModel(
    val recordId: Long,
    val templateId: Long,
    val images: List<String>,
    val timestamp: String,
    val title: String,
    val macros: MacrosUIModel?,
) : BaseListItemUIModel(id = recordId, contentType = "record")

data class MacrosUIModel(
    val calories: String?,
    val protein: String?,
    val fat: String?,
    val carbs: String?,
    val salt: String?,
    val fibre: String?,
)
