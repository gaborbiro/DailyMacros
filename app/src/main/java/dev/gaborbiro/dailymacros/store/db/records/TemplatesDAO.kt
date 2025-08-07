package dev.gaborbiro.dailymacros.store.db.records

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.gaborbiro.dailymacros.store.db.records.model.TemplateDBModel

@Dao
interface TemplatesDAO {

    @Upsert
    suspend fun insertOrUpdate(template: TemplateDBModel): Long

    @Query("SELECT * FROM templates WHERE _id=:id")
    suspend fun get(id: Long): TemplateDBModel?

    @Query("SELECT * FROM templates")
    suspend fun get(): List<TemplateDBModel>

    @Query("SELECT * FROM templates WHERE image=:image")
    suspend fun get(image: String?): List<TemplateDBModel>

    @Query("DELETE FROM templates WHERE _id = :id")
    suspend fun delete(id: Long): Int

    @Query("SELECT templates._id, templates.image, templates.name, templates.description, COUNT(records._id) as count FROM templates LEFT JOIN records ON templates._id = records.templateId GROUP BY templates._id ORDER BY count DESC")
    suspend fun getByFrequency(): List<TemplateDBModel>
}
