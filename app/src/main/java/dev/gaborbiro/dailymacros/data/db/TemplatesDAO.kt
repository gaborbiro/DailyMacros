package dev.gaborbiro.dailymacros.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.gaborbiro.dailymacros.data.db.model.TemplateJoined
import dev.gaborbiro.dailymacros.data.db.model.entity.ImageEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.MacrosEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity

@Dao
interface TemplatesDAO {

    @Upsert
    suspend fun insertOrUpdate(template: TemplateEntity): Long

    @Upsert
    suspend fun insertOrUpdate(macros: MacrosEntity): Long

    @Query("DELETE FROM macros WHERE templateId = :templateId")
    suspend fun deleteMacrosForTemplate(templateId: Long): Int

    @Transaction
    @Query("SELECT * FROM templates WHERE _id = :id")
    suspend fun getTemplateById(id: Long): TemplateJoined

    @Query("DELETE FROM templates WHERE _id = :id")
    suspend fun delete(id: Long): Int

    @Query(
        """
        SELECT T.*
        FROM templates T
        LEFT JOIN records R ON R.templateId = T._id
        GROUP BY T._id
        HAVING COUNT(R._id) > 1
        ORDER BY COUNT(R._id) DESC
        LIMIT :count
    """
    )
    suspend fun getQuickPicks(count: Int): List<TemplateJoined>

    // ---- IMAGES ----

    @Upsert
    suspend fun upsertImage(image: ImageEntity): Long

    @Insert
    suspend fun insertImages(images: List<ImageEntity>)

    @Query("SELECT * FROM template_images WHERE templateId = :templateId ORDER BY sortOrder ASC")
    suspend fun getImagesForTemplate(templateId: Long): List<ImageEntity>

    @Query("DELETE FROM template_images WHERE _id = :imageId")
    suspend fun deleteImage(imageId: Long): Int

    @Query("DELETE FROM template_images WHERE templateId = :templateId")
    suspend fun deleteAllImagesForTemplate(templateId: Long): Int

    @Query("SELECT COUNT(*) FROM template_images WHERE image = :image")
    suspend fun countTemplatesByImage(image: String): Int

    @Query("UPDATE template_images SET sortOrder = :sort WHERE _id = :imageId")
    suspend fun setSortOrder(imageId: Long, sort: Int): Int
}
