package dev.gaborbiro.dailymacros.data.db.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "variability_variants",
    foreignKeys = [
        ForeignKey(
            entity = VariabilitySlotEntity::class,
            parentColumns = ["_id"],
            childColumns = ["slotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["slotId"])],
)
data class VariabilityVariantEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Long = 0,
    @ColumnInfo(name = "slotId") val slotId: Long,
    @ColumnInfo(name = "variantKey") val variantKey: String,
    @ColumnInfo(name = "variantLabel") val variantLabel: String,
    @ColumnInfo(name = "notesExcerpt") val notesExcerpt: String,
    @ColumnInfo(name = "sortOrder") val sortOrder: Int,
)
