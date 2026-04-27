package dev.gaborbiro.dailymacros.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilityArchetypeEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilitySnapshotEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilitySlotEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilityVariantEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilityVariantEvidenceEntity

@Dao
abstract class VariabilityDao {

    @Query("SELECT * FROM variability_snapshots ORDER BY minedAtEpochMs DESC LIMIT 1")
    abstract suspend fun getLatestSnapshot(): VariabilitySnapshotEntity?

    @Query("DELETE FROM variability_snapshots")
    abstract suspend fun deleteAllSnapshots()

    @Insert
    abstract suspend fun insertSnapshot(entity: VariabilitySnapshotEntity): Long

    @Insert
    abstract suspend fun insertArchetype(entity: VariabilityArchetypeEntity): Long

    @Insert
    abstract suspend fun insertSlot(entity: VariabilitySlotEntity): Long

    @Insert
    abstract suspend fun insertVariant(entity: VariabilityVariantEntity): Long

    @Insert
    abstract suspend fun insertEvidenceRows(rows: List<VariabilityVariantEvidenceEntity>)

    @Transaction
    open suspend fun replaceEntireProfile(
        snapshot: VariabilitySnapshotEntity,
        archetypes: List<VariabilityArchetypeInsert>,
    ) {
        deleteAllSnapshots()
        val snapshotId = insertSnapshot(snapshot)
        for (a in archetypes) {
            val archetypeId = insertArchetype(a.archetype.copy(snapshotId = snapshotId))
            for (s in a.slots) {
                val slotId = insertSlot(s.slot.copy(archetypeId = archetypeId))
                for (v in s.variants) {
                    val variantId = insertVariant(v.variant.copy(slotId = slotId))
                    if (v.evidence.isNotEmpty()) {
                        insertEvidenceRows(
                            v.evidence.map { it.copy(variantId = variantId) },
                        )
                    }
                }
            }
        }
    }
}

data class VariabilityArchetypeInsert(
    val archetype: VariabilityArchetypeEntity,
    val slots: List<VariabilitySlotInsert>,
)

data class VariabilitySlotInsert(
    val slot: VariabilitySlotEntity,
    val variants: List<VariabilityVariantInsert>,
)

data class VariabilityVariantInsert(
    val variant: VariabilityVariantEntity,
    val evidence: List<VariabilityVariantEvidenceEntity>,
)
