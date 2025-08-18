package dev.gaborbiro.dailymacros.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.gaborbiro.dailymacros.data.db.model.entity.RecordEntity
import dev.gaborbiro.dailymacros.data.db.model.RecordJoined
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface RecordsDAO {

    @Upsert
    suspend fun insertOrUpdate(record: RecordEntity): Long

    @Transaction
    @Query("SELECT * FROM records ORDER BY timestamp DESC")
    suspend fun get(): List<RecordJoined>

    @Transaction
    @Query("SELECT * FROM records WHERE templateId=:templateId ORDER BY timestamp DESC")
    suspend fun getByTemplate(templateId: Long): List<RecordJoined>

    @Transaction
    @Query("SELECT * FROM records WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getFlow(since: LocalDateTime?): Flow<List<RecordJoined>>

    @Transaction
    @Query(
        """
        SELECT * FROM records
        WHERE timestamp >= :since AND timestamp < :until
        ORDER BY timestamp DESC
    """
    )
    fun getFlow(
        since: LocalDateTime?,
        until: LocalDateTime,
    ): Flow<List<RecordJoined>>

    @Transaction
    @Query(
        """
        SELECT R.*
        FROM records R
        WHERE R.templateId IN (
            SELECT T._id FROM templates T
            WHERE T.name LIKE '%' || :search || '%'
               OR T.description LIKE '%' || :search || '%'
        )
        ORDER BY R.timestamp DESC
    """
    )
    fun getFlowBySearchTerm(search: String): Flow<List<RecordJoined>>

    @Transaction
    @Query("SELECT * FROM records WHERE _id=:id")
    suspend fun get(id: Long): RecordJoined

    @Transaction
    @Query("DELETE FROM records WHERE _id = :id")
    suspend fun delete(id: Long): Int
}
