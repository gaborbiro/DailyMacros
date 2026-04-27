package dev.gaborbiro.dailymacros.repositories.records

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import dev.gaborbiro.dailymacros.data.db.VariabilityArchetypeInsert
import dev.gaborbiro.dailymacros.data.db.VariabilityDao
import dev.gaborbiro.dailymacros.data.db.VariabilitySlotInsert
import dev.gaborbiro.dailymacros.data.db.VariabilityVariantInsert
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilityArchetypeEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilitySnapshotEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilitySlotEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilityVariantEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilityVariantEvidenceEntity

/**
 * Parses merged variability profile JSON from the LLM and replaces normalized DB rows.
 */
class VariabilityProfilePersister(
    private val gson: Gson,
    private val variabilityDao: VariabilityDao,
) {

    suspend fun persist(profileJson: String, minedAtEpochMs: Long) {
        val root = gson.fromJson(profileJson, JsonObject::class.java)
        val archetypesJson = root.getAsJsonArray("archetypes") ?: JsonArray()
        val archetypes = mutableListOf<VariabilityArchetypeInsert>()
        var archeOrder = 0
        for (aEl in archetypesJson) {
            if (!aEl.isJsonObject) continue
            val a = aEl.asJsonObject
            val archetypeKey = a.get("archetype_id")?.asString ?: continue
            val displayName = optString(a, "display_name") ?: archetypeKey
            val titleAliases = a.get("title_aliases") ?: JsonArray()
            val archetypeEntity = VariabilityArchetypeEntity(
                snapshotId = 0L,
                archetypeKey = archetypeKey,
                displayName = displayName,
                titleAliasesJson = gson.toJson(titleAliases),
                evidenceCount = a.get("evidence_count")?.asInt ?: 0,
                lastSeenTimestamp = optString(a, "last_seen_timestamp"),
                archetypeNotes = optString(a, "archetype_notes"),
                deprecated = a.get("deprecated")?.asBoolean ?: false,
                deprecatedReason = optString(a, "deprecated_reason"),
                sortOrder = archeOrder++,
            )
            val slots = mutableListOf<VariabilitySlotInsert>()
            val slotsJson = a.getAsJsonArray("slots") ?: JsonArray()
            var slotOrder = 0
            for (sEl in slotsJson) {
                if (!sEl.isJsonObject) continue
                val s = sEl.asJsonObject
                val slotKey = s.get("slot_id")?.asString ?: continue
                val role = optString(s, "role") ?: slotKey
                val levers = s.getAsJsonArray("nutritional_levers") ?: JsonArray()
                val slotEntity = VariabilitySlotEntity(
                    archetypeId = 0L,
                    slotKey = slotKey,
                    role = role,
                    nutritionalLeversJson = gson.toJson(levers),
                    isHighVariability = s.get("is_high_variability")?.asBoolean ?: false,
                    confidence = s.get("confidence")?.asDouble ?: 0.0,
                    rationale = optString(s, "rationale").orEmpty(),
                    sortOrder = slotOrder++,
                )
                val variants = mutableListOf<VariabilityVariantInsert>()
                val variantsJson = s.getAsJsonArray("variants") ?: JsonArray()
                var variantOrder = 0
                for (vEl in variantsJson) {
                    if (!vEl.isJsonObject) continue
                    val v = vEl.asJsonObject
                    val variantKey = v.get("variant_id")?.asString ?: continue
                    val typical = v.get("typical_macros")
                    val variantEntity = VariabilityVariantEntity(
                        slotId = 0L,
                        variantKey = variantKey,
                        variantLabel = optString(v, "variant_label").orEmpty(),
                        macroSource = optString(v, "macro_source").orEmpty(),
                        notesExcerpt = optString(v, "notes_excerpt").orEmpty(),
                        typicalMacrosJson = if (typical != null) gson.toJson(typical) else "{}",
                        sortOrder = variantOrder++,
                    )
                    val evidence = parseEvidence(v)
                    variants.add(VariabilityVariantInsert(variantEntity, evidence))
                }
                slots.add(VariabilitySlotInsert(slotEntity, variants))
            }
            archetypes.add(VariabilityArchetypeInsert(archetypeEntity, slots))
        }
        variabilityDao.replaceEntireProfile(
            VariabilitySnapshotEntity(
                minedAtEpochMs = minedAtEpochMs,
                profileJson = profileJson,
            ),
            archetypes,
        )
    }

    private fun parseEvidence(v: JsonObject): List<VariabilityVariantEvidenceEntity> {
        val evidenceEl = v.get("supporting_entry_evidence")
        if (evidenceEl != null && evidenceEl.isJsonArray) {
            return parseEvidenceArray(evidenceEl.asJsonArray)
        }
        val legacy = v.get("supporting_entry_timestamps")
        if (legacy != null && legacy.isJsonArray) {
            return legacy.asJsonArray.mapNotNull { e ->
                if (e.isJsonPrimitive && e.asJsonPrimitive.isString) {
                    VariabilityVariantEvidenceEntity(
                        variantId = 0L,
                        loggedAt = e.asString,
                        templateId = null,
                    )
                } else {
                    null
                }
            }
        }
        return emptyList()
    }

    private fun parseEvidenceArray(arr: JsonArray): List<VariabilityVariantEvidenceEntity> {
        val out = mutableListOf<VariabilityVariantEvidenceEntity>()
        for (e in arr) {
            when {
                e.isJsonPrimitive && e.asJsonPrimitive.isString -> {
                    out.add(
                        VariabilityVariantEvidenceEntity(
                            variantId = 0L,
                            loggedAt = e.asString,
                            templateId = null,
                        ),
                    )
                }
                e.isJsonObject -> {
                    val o = e.asJsonObject
                    val loggedAt = optString(o, "logged_at") ?: continue
                    val templateId = when (val t = o.get("template_id")) {
                        null -> null
                        is JsonPrimitive -> if (t.isNumber) t.asLong else null
                        else -> null
                    }
                    out.add(
                        VariabilityVariantEvidenceEntity(
                            variantId = 0L,
                            loggedAt = loggedAt,
                            templateId = templateId,
                        ),
                    )
                }
            }
        }
        return out
    }
}

private fun optString(o: JsonObject, key: String): String? =
    o.get(key)?.takeIf { it.isJsonPrimitive && !it.isJsonNull }?.asString
