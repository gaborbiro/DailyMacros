package dev.gaborbiro.dailymacros.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.gaborbiro.dailymacros.data.db.model.entity.NutrientsEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.ImageEntity
import dev.gaborbiro.dailymacros.data.db.model.TemplateJoined

@Dao
interface TemplatesDAO {

    @Upsert
    suspend fun insertOrUpdate(template: TemplateEntity): Long

    @Upsert
    suspend fun insertOrUpdate(nutrients: NutrientsEntity): Long

    @Query("DELETE FROM nutrients WHERE templateId = :templateId")
    suspend fun deleteNutrientsForTemplate(templateId: Long): Int

    @Transaction
    suspend fun upsertTemplateWithNutrients(
        template: TemplateEntity,
        nutrients: NutrientsEntity?,
    ): TemplateJoined {
        val rid = insertOrUpdate(template)
        val templateId = if (rid == -1L) requireNotNull(template.id) else rid
        if (nutrients == null) {
            deleteNutrientsForTemplate(templateId)
        } else {
            insertOrUpdate(nutrients.copy(templateId = templateId))
        }
        return getTemplateById(templateId)
    }

    @Transaction
    @Query("SELECT * FROM templates WHERE _id = :id")
    suspend fun getTemplateById(id: Long): TemplateJoined

    @Query("SELECT * FROM templates")
    suspend fun getAll(): List<TemplateJoined>

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
        LIMIT 10
    """
    )
    suspend fun getTop10(): List<TemplateJoined>

    // ---- IMAGES ----

    @Upsert
    suspend fun upsertImage(image: ImageEntity): Long

    @Insert
    suspend fun insertImages(images: List<ImageEntity>)

    @Query("SELECT * FROM template_images WHERE templateId = :templateId ORDER BY sortOrder ASC")
    suspend fun getImagesForTemplate(templateId: Long): List<ImageEntity>

    @Query("SELECT * FROM template_images WHERE _id = :imageId")
    suspend fun getImageById(imageId: Long): ImageEntity?

    @Query("DELETE FROM template_images WHERE _id = :imageId")
    suspend fun deleteImage(imageId: Long): Int

    @Query("DELETE FROM template_images WHERE templateId = :templateId")
    suspend fun deleteAllImagesForTemplate(templateId: Long): Int

    @Query("SELECT COUNT(*) FROM template_images WHERE image = :image")
    suspend fun countByUri(image: String): Int

    // Make exactly one primary by flipping flags in one SQL hit
    @Query(
        """
        UPDATE template_images
        SET isPrimary = (_id = :imageId)
        WHERE templateId = :templateId
    """
    )
    suspend fun setPrimary(templateId: Long, imageId: Long): Int

    @Query("UPDATE template_images SET sortOrder = :sort WHERE _id = :imageId")
    suspend fun setSortOrder(imageId: Long, sort: Int): Int
}
