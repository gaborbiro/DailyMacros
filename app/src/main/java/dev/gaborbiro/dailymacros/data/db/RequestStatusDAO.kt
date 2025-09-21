package dev.gaborbiro.dailymacros.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.gaborbiro.dailymacros.data.db.model.entity.RequestStatus
import dev.gaborbiro.dailymacros.data.db.model.entity.RequestStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RequestStatusDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(status: RequestStatusEntity): Long

    @Query("SELECT * FROM request_status")
    suspend fun getAll(): List<RequestStatusEntity>

    @Query("SELECT * FROM request_status")
    fun observeAll(): Flow<List<RequestStatusEntity>>

    @Query("SELECT * FROM request_status WHERE templateId=:templateId")
    suspend fun getByTemplate(templateId: Long): RequestStatusEntity?

    @Query("DELETE FROM request_status WHERE templateId=:templateId")
    suspend fun deleteByTemplate(templateId: Long): Int

    @Query("DELETE FROM request_status WHERE status = :status AND startedAt < :cutoff")
    suspend fun deleteStale(status: RequestStatus, cutoff: Long)

    @Delete
    suspend fun delete(vararg entries: RequestStatusEntity): Int
}
