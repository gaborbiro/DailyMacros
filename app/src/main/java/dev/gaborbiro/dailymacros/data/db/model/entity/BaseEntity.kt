package dev.gaborbiro.dailymacros.data.db.model.entity

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

const val COLUMN_ID = "_id"

abstract class BaseEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID)
    var id: Long? = null
}
