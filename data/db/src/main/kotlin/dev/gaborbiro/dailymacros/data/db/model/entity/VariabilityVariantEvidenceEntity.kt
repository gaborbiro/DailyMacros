package dev.gaborbiro.dailymacros.data.db.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "variability_variant_evidence",
    foreignKeys = [
        ForeignKey(
            entity = VariabilityVariantEntity::class,
            parentColumns = ["_id"],
            childColumns = ["variantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["variantId"]), Index(value = ["templateId"])],
)
data class VariabilityVariantEvidenceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Long = 0,
    @ColumnInfo(name = "variantId") val variantId: Long,
    @ColumnInfo(name = "loggedAt") val loggedAt: String,
    @ColumnInfo(name = "templateId") val templateId: Long?,
)
