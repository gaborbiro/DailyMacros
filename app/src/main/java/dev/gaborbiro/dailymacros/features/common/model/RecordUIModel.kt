package dev.gaborbiro.dailymacros.features.common.model

import androidx.compose.runtime.Stable

@Stable
data class RecordUIModel(
    val recordId: Long,
    val templateId: Long,
    val images: List<String>,
    val timestamp: String,
    val title: String,
    val description: String,
    val hasMacros: Boolean,
) : BaseListItemUIModel(recordId, "record")
