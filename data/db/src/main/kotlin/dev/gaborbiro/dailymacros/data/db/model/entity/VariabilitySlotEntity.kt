package dev.gaborbiro.dailymacros.data.db.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "variability_slots",
    foreignKeys = [
        ForeignKey(
            entity = VariabilityArchetypeEntity::class,
            parentColumns = ["_id"],
            childColumns = ["archetypeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["archetypeId"])],
)
data class VariabilitySlotEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Long = 0,
    @ColumnInfo(name = "archetypeId") val archetypeId: Long,
    @ColumnInfo(name = "slotKey") val slotKey: String,
    @ColumnInfo(name = "role") val role: String,
    @ColumnInfo(name = "nutritionalLeversJson") val nutritionalLeversJson: String,
    @ColumnInfo(name = "isHighVariability") val isHighVariability: Boolean,
    @ColumnInfo(name = "confidence") val confidence: Double,
    @ColumnInfo(name = "rationale") val rationale: String,
    @ColumnInfo(name = "sortOrder") val sortOrder: Int,
)
