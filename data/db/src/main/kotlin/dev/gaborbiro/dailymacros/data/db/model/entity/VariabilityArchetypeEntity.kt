package dev.gaborbiro.dailymacros.data.db.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "variability_archetypes",
    foreignKeys = [
        ForeignKey(
            entity = VariabilitySnapshotEntity::class,
            parentColumns = ["_id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["snapshotId"])],
)
data class VariabilityArchetypeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Long = 0,
    @ColumnInfo(name = "snapshotId") val snapshotId: Long,
    @ColumnInfo(name = "archetypeKey") val archetypeKey: String,
    @ColumnInfo(name = "displayName") val displayName: String,
    @ColumnInfo(name = "titleAliasesJson") val titleAliasesJson: String,
    @ColumnInfo(name = "evidenceCount") val evidenceCount: Int,
    @ColumnInfo(name = "lastSeenTimestamp") val lastSeenTimestamp: String?,
    @ColumnInfo(name = "archetypeNotes") val archetypeNotes: String?,
    @ColumnInfo(name = "deprecated") val deprecated: Boolean,
    @ColumnInfo(name = "deprecatedReason") val deprecatedReason: String?,
    @ColumnInfo(name = "sortOrder") val sortOrder: Int,
)
