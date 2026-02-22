package dev.gaborbiro.dailymacros.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.gaborbiro.dailymacros.data.db.model.TemplateJoined
import dev.gaborbiro.dailymacros.data.db.model.entity.ImageEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.MacrosEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.QuickPickOverrideEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TopContributorsEntity

@Dao
interface TemplatesDAO {

    @Upsert
    suspend fun insertOrUpdate(template: TemplateEntity): Long

    @Upsert
    suspend fun insertOrUpdate(macros: MacrosEntity): Long

    @Upsert
    suspend fun insertOrUpdate(topContributors: TopContributorsEntity): Long

    @Query("DELETE FROM macros WHERE templateId = :templateId")
    suspend fun deleteMacrosForTemplate(templateId: Long): Int

    @Transaction
    @Query("SELECT * FROM templates WHERE _id = :id")
    suspend fun getTemplateById(id: Long): TemplateJoined

    @Query("DELETE FROM templates WHERE _id = :id")
    suspend fun delete(id: Long): Int

    @Transaction
    @Query(
        """
WITH Usage AS (
    SELECT
        templateId,
        COUNT(*) AS usageCount
    FROM records
    GROUP BY templateId
),
Base AS (
    SELECT
        T.*,
        COALESCE(U.usageCount, 0) AS usageCount,
        QO.overrideType
    FROM templates T
    LEFT JOIN Usage U
        ON U.templateId = T._id
    LEFT JOIN QuickPickOverride QO
        ON QO.templateId = T._id
    WHERE COALESCE(QO.overrideType, '') != 'EXCLUDE'
),
Ranked AS (
    SELECT
        *,
        ROW_NUMBER() OVER (
            PARTITION BY (overrideType IS NULL)
            ORDER BY usageCount DESC
        ) AS regularRank
    FROM Base
)

SELECT *
FROM Ranked
WHERE overrideType = 'INCLUDE'
   OR (overrideType IS NULL AND regularRank <= :count)
ORDER BY
    CASE WHEN overrideType = 'INCLUDE' THEN 0 ELSE 1 END,
    usageCount DESC;
    """
    )
    suspend fun getQuickPicks(count: Int): List<TemplateJoined>

    // ---- QUICK PICK OVERRIDES ----

    @Upsert
    suspend fun upsertQuickPickOverride(override: QuickPickOverrideEntity)

    @Query("DELETE FROM QuickPickOverride WHERE templateId = :templateId")
    suspend fun deleteQuickPickOverride(templateId: Long)

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
