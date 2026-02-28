package dev.gaborbiro.dailymacros.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.gaborbiro.dailymacros.data.db.model.RecordJoined
import dev.gaborbiro.dailymacros.data.db.model.entity.RecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordsDAO {

    @Upsert
    suspend fun insertOrUpdate(record: RecordEntity): Long

    @Transaction
    @Query("SELECT * FROM records WHERE epochMillis>=:sinceEpochMillis ORDER BY epochMillis DESC")
    suspend fun get(sinceEpochMillis: Long): List<RecordJoined>

    @Transaction
    @Query("SELECT * FROM records ORDER BY epochMillis DESC LIMIT 1")
    fun getMostRecentRecord(): RecordJoined?

    @Transaction
    @Query("SELECT * FROM records WHERE templateId=:templateId ORDER BY epochMillis DESC")
    suspend fun getByTemplate(templateId: Long): List<RecordJoined>

    @Transaction
    @Query("SELECT * FROM records WHERE epochMillis>=:sinceEpochMillis ORDER BY epochMillis")
    fun getFlow(sinceEpochMillis: Long?): Flow<List<RecordJoined>>

    @Transaction
    @Query(
        """
        SELECT * FROM records
        WHERE epochMillis>=:sinceEpochMillis AND epochMillis<:untilEpochMillis
        ORDER BY epochMillis DESC
        """
    )
    fun getFlow(
        sinceEpochMillis: Long?,
        untilEpochMillis: Long,
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
        ORDER BY R.epochMillis
        """
    )
    fun getFlowBySearchTerm(search: String): Flow<List<RecordJoined>>

    @Transaction
    @Query("SELECT * FROM records WHERE _id=:id")
    suspend fun getById(id: Long): RecordJoined?

    @Transaction
    @Query("SELECT * FROM records WHERE _id=:id")
    fun observe(id: Long): Flow<RecordJoined>

    @Transaction
    @Query("DELETE FROM records WHERE _id = :id")
    suspend fun delete(id: Long): Int
}
