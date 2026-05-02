package dev.gaborbiro.dailymacros.data.db.model

import androidx.room.ColumnInfo

/** Earliest log time per template (for lineage ordering). */
data class TemplateFirstSeenRow(
    @ColumnInfo(name = "templateId") val templateId: Long,
    @ColumnInfo(name = "firstSeenEpochMs") val firstSeenEpochMs: Long,
)
