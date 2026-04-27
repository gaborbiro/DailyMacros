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
)
