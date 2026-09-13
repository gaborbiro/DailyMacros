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

    @Query("SELECT COUNT(*) FROM records WHERE templateId = :templateId")
    suspend fun countByTemplate(templateId: Long): Int

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
    @Query("SELECT * FROM records WHERE _id=:id")
    suspend fun getById(id: Long): RecordJoined?

    // Nullable, not RecordJoined: a non-null singular Flow<T> tells Room to
    // throw IllegalStateException if the query ever returns zero rows (e.g.
    // this id was deleted, or - see the widget/deep-link crash this fixed -
    // never existed at all after a restore swapped in a different database).
    // Nullable lets a vanished/nonexistent row just emit null instead.
    @Transaction
    @Query("SELECT * FROM records WHERE _id=:id")
    fun observe(id: Long): Flow<RecordJoined?>

    @Transaction
    @Query("DELETE FROM records WHERE _id = :id")
    suspend fun delete(id: Long): Int
}
