package dev.gaborbiro.dailymacros.data.db.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "variability_snapshots")
data class VariabilitySnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Long = 0,
    @ColumnInfo(name = "minedAtEpochMs") val minedAtEpochMs: Long,
    /** Full merged profile JSON from the model (audit / future re-parse). */
    @ColumnInfo(name = "profileJson") val profileJson: String,
    /**
     * Max of `max(createdAtEpochMs, updatedAtEpochMs)` over templates that were included in this
     * mine’s [meal_observations]. Next run sends only templates newer than this watermark.
     */
    @ColumnInfo(name = "templatesIngestWatermarkEpochMs") val templatesIngestWatermarkEpochMs: Long = 0L,
)
