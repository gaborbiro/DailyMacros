package dev.gaborbiro.nutri.store.db.common

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

const val COLUMN_ID = "_id"

abstract class DBModel {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID)
    var id: Long? = null
}
