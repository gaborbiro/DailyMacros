package dev.gaborbiro.dailymacros.store.db.records

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.gaborbiro.dailymacros.store.db.records.model.RecordAndTemplateDBModel
import dev.gaborbiro.dailymacros.store.db.records.model.RecordDBModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface RecordsDAO {

    @Upsert
    @Transaction
    suspend fun insertOrUpdate(record: RecordDBModel): Long

    @Transaction
    @Query("SELECT * FROM records ORDER BY timestamp DESC")
    suspend fun get(): List<RecordAndTemplateDBModel>

    @Transaction
    @Query("SELECT * FROM records WHERE templateId=:templateId ORDER BY timestamp DESC")
    suspend fun getByTemplate(templateId: Long): List<RecordAndTemplateDBModel>

    @Transaction
    @Query("SELECT * FROM records WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getFlow(since: LocalDateTime? = LocalDateTime.MIN): Flow<List<RecordAndTemplateDBModel>>

    @Transaction
    @Query("SELECT * FROM records WHERE timestamp >= :since AND timestamp < :until ORDER BY timestamp DESC")
    fun getFlow(since: LocalDateTime? = LocalDateTime.MIN, until: LocalDateTime): Flow<List<RecordAndTemplateDBModel>>

    @Transaction
    @Query("SELECT * FROM records LEFT JOIN templates ON templates._id = records.templateId WHERE name LIKE '%' || :search || '%' OR description LIKE '%' || :search || '%' ORDER BY timestamp DESC")
    fun getFlowBySearchTerm(search: String): Flow<List<RecordAndTemplateDBModel>>

    @Transaction
    @Query("SELECT * FROM records WHERE _id=:id")
    suspend fun get(id: Long): RecordAndTemplateDBModel?

    @Transaction
    @Query("DELETE FROM records WHERE _id = :id")
    suspend fun delete(id: Long): Int
}
