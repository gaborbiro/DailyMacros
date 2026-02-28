package dev.gaborbiro.dailymacros.data.db.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(
    tableName = "QuickPickOverride",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = arrayOf(COLUMN_ID),
            childColumns = arrayOf(QuickPickOverrideEntity.COLUMN_TEMPLATE_ID),
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class QuickPickOverrideEntity(
    @PrimaryKey
    @ColumnInfo(name = COLUMN_TEMPLATE_ID) val templateId: Long,
    val overrideType: OverrideType,
    val sortOrder: Int? = null,
) {
    companion object {
        const val COLUMN_TEMPLATE_ID = "templateId"
    }

    enum class OverrideType {
        INCLUDE, EXCLUDE
    }

    class Converters {
        @TypeConverter
        fun from(overrideType: OverrideType): String = overrideType.name

        @TypeConverter
        fun to(value: String): OverrideType = OverrideType.valueOf(value)
    }
}
