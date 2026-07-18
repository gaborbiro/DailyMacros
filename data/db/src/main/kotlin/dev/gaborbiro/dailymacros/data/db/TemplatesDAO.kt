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
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplatesDAO {

    @Upsert
    suspend fun insertOrUpdate(template: TemplateEntity): Long

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun countAllTemplates(): Int

    /**
     * Templates in the same explicit variant family as [templateId]: walk up `parentTemplateId` to
     * the root, then include the root and every descendant template.
     */
    @Query(
        """
        WITH RECURSIVE ancestors AS (
            SELECT _id, parentTemplateId FROM templates WHERE _id = :templateId
            UNION ALL
            SELECT t._id, t.parentTemplateId FROM templates t
            INNER JOIN ancestors a ON t._id = a.parentTemplateId
        ),
        tree AS (
            SELECT a._id FROM ancestors a WHERE a.parentTemplateId IS NULL
            UNION ALL
            SELECT t._id FROM templates t
            INNER JOIN tree ON t.parentTemplateId = tree._id
        )
        SELECT _id FROM tree
        """,
    )
    suspend fun getTemplateIdsInVariantFamily(templateId: Long): List<Long>

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
   OR (overrideType IS NULL AND regularRank <= :count AND usageCount >= 2)
ORDER BY
    CASE WHEN overrideType = 'INCLUDE' THEN 0 ELSE 1 END,
    usageCount DESC;
    """
    )
    suspend fun getQuickPicks(count: Int): List<TemplateJoined>

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
   OR (overrideType IS NULL AND regularRank <= :count AND usageCount >= 2)
ORDER BY
    CASE WHEN overrideType = 'INCLUDE' THEN 0 ELSE 1 END,
    usageCount DESC;
    """
    )
    fun observeQuickPicks(count: Int): Flow<List<TemplateJoined>>

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

    @Query("SELECT DISTINCT sourceMediaStoreId FROM template_images WHERE sourceMediaStoreId IS NOT NULL")
    suspend fun getSourceMediaStoreIds(): List<Long>

    @Query("SELECT sourceMediaStoreId FROM template_images WHERE image = :image AND sourceMediaStoreId IS NOT NULL LIMIT 1")
    suspend fun getSourceMediaStoreIdForImage(image: String): Long?

    @Query("UPDATE template_images SET sortOrder = :sort WHERE _id = :imageId")
    suspend fun setSortOrder(imageId: Long, sort: Int): Int
}
