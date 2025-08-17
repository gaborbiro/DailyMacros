package dev.gaborbiro.dailymacros.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.gaborbiro.dailymacros.data.db.model.NutrientsDBModel
import dev.gaborbiro.dailymacros.data.db.model.TemplateDBModel
import dev.gaborbiro.dailymacros.data.db.model.TemplateWithNutrients

@Dao
interface TemplatesDAO {

    @Upsert
    suspend fun insertOrUpdate(template: TemplateDBModel): Long

    @Upsert
    suspend fun insertOrUpdate(nutrients: NutrientsDBModel): Long

    @Query("DELETE FROM nutrients WHERE templateId = :templateId")
    suspend fun deleteNutrientsForTemplate(templateId: Long): Int

    @Transaction
    suspend fun upsertTemplateWithNutrients(
        template: TemplateDBModel,
        nutrients: NutrientsDBModel?
    ): TemplateWithNutrients {
        val rid = insertOrUpdate(template)
        val templateId = if (rid == -1L) requireNotNull(template.id) else rid
        if (nutrients == null) {
            deleteNutrientsForTemplate(templateId)
        } else {
            insertOrUpdate(nutrients.copy(templateId = templateId))
        }
        return get(templateId)
    }

    @Transaction
    @Query("SELECT * FROM templates WHERE _id = :id")
    suspend fun get(id: Long): TemplateWithNutrients

    @Query("SELECT * FROM templates")
    suspend fun getAll(): List<TemplateWithNutrients>

    @Query("SELECT * FROM templates WHERE image=:image")
    suspend fun getByImage(image: String?): List<TemplateWithNutrients>

    @Query("DELETE FROM templates WHERE _id = :id")
    suspend fun delete(id: Long): Int

    @Query("""
        SELECT T.*
        FROM templates T
        LEFT JOIN records R ON R.templateId = T._id
        GROUP BY T._id
        HAVING COUNT(R._id) > 1
        ORDER BY COUNT(R._id) DESC
    """)
    suspend fun getByFrequency(): List<TemplateDBModel>
}
