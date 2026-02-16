package dev.gaborbiro.dailymacros.features.common.model

import androidx.compose.runtime.Stable

@Stable
data class ListUiModelRecord(
    val recordId: Long,
    val templateId: Long,
    val images: List<String>,
    val timestamp: String,
    val title: String,
    val macrosAmounts: MacrosAmountsUIModel?,
    val showLoadingIndicator: Boolean,
    val showAddToQuickPicksMenuItem: Boolean,
) : ListUiModelBase(listItemId = recordId, contentType = "record")

data class MacrosAmountsUIModel(
    val calories: String?,
    val protein: String?,
    val fat: String?,
    val carbs: String?,
    val salt: String?,
    val fibre: String?,
)
