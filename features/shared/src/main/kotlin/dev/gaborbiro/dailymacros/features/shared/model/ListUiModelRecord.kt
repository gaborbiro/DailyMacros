package dev.gaborbiro.dailymacros.features.shared.model

import androidx.compose.runtime.Stable

@Stable
data class ListUiModelRecord(
    val recordId: Long,
    val templateId: Long,
    val imageFilename: String?,
    val timestamp: String,
    val title: String,
    val nutrients: NutrientsUiModel?,
    val showLoadingIndicator: Boolean,
    /** True when other logged templates exist in the same variant family (overview title-row icon). */
    val showOtherLoggedVariantsIcon: Boolean = false,
) : ListUiModelBase(listItemId = recordId, contentType = "record")
