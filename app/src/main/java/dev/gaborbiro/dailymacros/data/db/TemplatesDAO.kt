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

    @Transaction
    @Query(
        """
WITH Base AS (
    SELECT
        T.*,
        QO.overrideType AS overrideType,
        COUNT(R._id)   AS recordCount,

        -- materialized sort keys (must be real columns for ORDER BY with UNION)
        CASE WHEN QO.overrideType = 'INCLUDE' THEN 0 ELSE 1 END AS includeRank,
        COALESCE(QO.sortOrder, 999999) AS sortOrderKey
    FROM templates T
    LEFT JOIN records R ON R.templateId = T._id
    LEFT JOIN QuickPickOverride QO ON QO.templateId = T._id
    WHERE COALESCE(QO.overrideType, '') != 'EXCLUDE'
    GROUP BY T._id
    HAVING COUNT(R._id) > 1 OR QO.overrideType = 'INCLUDE'
),
IncludeCount AS (
    SELECT COUNT(*) AS cnt
    FROM Base
    WHERE overrideType = 'INCLUDE'
)

SELECT *
FROM (
    SELECT *
    FROM Base
    WHERE overrideType = 'INCLUDE'

    UNION ALL

    SELECT *
    FROM Base
    WHERE overrideType IS NULL OR overrideType != 'INCLUDE'
    ORDER BY
        sortOrderKey,
        recordCount DESC
    LIMIT MAX(:count - (SELECT cnt FROM IncludeCount), 0)
)
ORDER BY
    includeRank,
    sortOrderKey,
    recordCount DESC
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
