package dev.gaborbiro.dailymacros.repositories.records

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import dev.gaborbiro.dailymacros.data.db.VariabilityArchetypeInsert
import dev.gaborbiro.dailymacros.data.db.VariabilitySlotInsert
import dev.gaborbiro.dailymacros.data.db.VariabilityVariantInsert
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilityArchetypeEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilitySnapshotEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilitySlotEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilityVariantEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilityVariantEvidenceEntity
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.MealVariabilityPersistedProfile
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityArchetype
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityEvidence
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilitySlot
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityVariant

/**
 * JSON profile ↔ domain and domain ↔ Room insert rows.
 */
class VariabilityProfileMapper(
    private val gson: Gson,
) {

    fun parseProfileJson(
        profileJson: String,
        minedAtEpochMs: Long,
        templatesIngestWatermarkEpochMs: Long = 0L,
    ): MealVariabilityPersistedProfile {
        val root = gson.fromJson(profileJson, JsonObject::class.java)
        val archetypesJson = root.getAsJsonArray("archetypes") ?: JsonArray()
        val archetypes = mutableListOf<VariabilityArchetype>()
        var archeOrder = 0
        for (aEl in archetypesJson) {
            if (!aEl.isJsonObject) continue
            val a = aEl.asJsonObject
            val archetypeKey = a.get("archetype_id")?.asString ?: continue
            val displayName = optString(a, "display_name") ?: archetypeKey
            val titleAliases = a.get("title_aliases") ?: JsonArray()
            val slots = mutableListOf<VariabilitySlot>()
            val slotsJson = a.getAsJsonArray("slots") ?: JsonArray()
            var slotOrder = 0
            for (sEl in slotsJson) {
                if (!sEl.isJsonObject) continue
                val s = sEl.asJsonObject
                val slotKey = s.get("slot_id")?.asString ?: continue
                val roleDisplayName = optString(s, "role_display_name")
                    ?: optString(s, "role")
                    ?: slotKey
                val levers = s.getAsJsonArray("nutritional_levers") ?: JsonArray()
                val variants = mutableListOf<VariabilityVariant>()
                val variantsJson = s.getAsJsonArray("variants") ?: JsonArray()
                var variantOrder = 0
                for (vEl in variantsJson) {
                    if (!vEl.isJsonObject) continue
                    val v = vEl.asJsonObject
                    val variantKey = v.get("variant_id")?.asString ?: continue
                    variants.add(
                        VariabilityVariant(
                            variantKey = variantKey,
                            variantLabel = optString(v, "variant_label").orEmpty(),
                            notesExcerpt = optString(v, "notes_excerpt").orEmpty(),
                            evidence = parseEvidenceDomain(v),
                            sortOrder = variantOrder++,
                        ),
                    )
                }
                slots.add(
                    VariabilitySlot(
                        slotKey = slotKey,
                        roleDisplayName = roleDisplayName,
                        nutritionalLeversJson = gson.toJson(levers),
                        isHighVariability = s.get("is_high_variability")?.asBoolean ?: false,
                        confidence = s.get("confidence")?.asDouble ?: 0.0,
                        rationale = optString(s, "rationale").orEmpty(),
                        variants = variants,
                        sortOrder = slotOrder++,
                    ),
                )
            }
            archetypes.add(
                VariabilityArchetype(
                    archetypeKey = archetypeKey,
                    displayName = displayName,
                    titleAliasesJson = gson.toJson(titleAliases),
                    evidenceCount = a.get("evidence_count")?.asInt ?: 0,
                    lastSeenTimestamp = optString(a, "last_seen_timestamp"),
                    archetypeNotes = optString(a, "archetype_notes"),
                    deprecated = a.get("deprecated")?.asBoolean ?: false,
                    deprecatedReason = optString(a, "deprecated_reason"),
                    slots = slots,
                    sortOrder = archeOrder++,
                ),
            )
        }
        return MealVariabilityPersistedProfile(
            minedAtEpochMs = minedAtEpochMs,
            profileJson = profileJson,
            archetypes = archetypes,
            templatesIngestWatermarkEpochMs = templatesIngestWatermarkEpochMs,
        )
    }

    fun toSnapshotAndInserts(profile: MealVariabilityPersistedProfile): Pair<VariabilitySnapshotEntity, List<VariabilityArchetypeInsert>> {
        val snapshot = VariabilitySnapshotEntity(
            minedAtEpochMs = profile.minedAtEpochMs,
            profileJson = profile.profileJson,
            templatesIngestWatermarkEpochMs = profile.templatesIngestWatermarkEpochMs,
        )
        val inserts = profile.archetypes.map { a ->
            VariabilityArchetypeInsert(
                archetype = VariabilityArchetypeEntity(
                    snapshotId = 0L,
                    archetypeKey = a.archetypeKey,
                    displayName = a.displayName,
                    titleAliasesJson = a.titleAliasesJson,
                    evidenceCount = a.evidenceCount,
                    lastSeenTimestamp = a.lastSeenTimestamp,
                    archetypeNotes = a.archetypeNotes,
                    deprecated = a.deprecated,
                    deprecatedReason = a.deprecatedReason,
                    sortOrder = a.sortOrder,
                ),
                slots = a.slots.map { s ->
                    VariabilitySlotInsert(
                        slot = VariabilitySlotEntity(
                            archetypeId = 0L,
                            slotKey = s.slotKey,
                            role = s.roleDisplayName,
                            nutritionalLeversJson = s.nutritionalLeversJson,
                            isHighVariability = s.isHighVariability,
                            confidence = s.confidence,
                            rationale = s.rationale,
                            sortOrder = s.sortOrder,
                        ),
                        variants = s.variants.map { v ->
                            VariabilityVariantInsert(
                                variant = VariabilityVariantEntity(
                                    slotId = 0L,
                                    variantKey = v.variantKey,
                                    variantLabel = v.variantLabel,
                                    notesExcerpt = v.notesExcerpt,
                                    sortOrder = v.sortOrder,
                                ),
                                evidence = v.evidence.map { e ->
                                    VariabilityVariantEvidenceEntity(
                                        variantId = 0L,
                                        loggedAt = e.loggedAt,
                                        templateId = e.templateId,
                                        mealTitle = e.title,
                                    )
                                },
                            )
                        },
                    )
                },
            )
        }
        return snapshot to inserts
    }

    private fun parseEvidenceDomain(v: JsonObject): List<VariabilityEvidence> {
        val evidenceEl = v.get("supporting_entry_evidence")
        if (evidenceEl != null && evidenceEl.isJsonArray) {
            return parseEvidenceArrayDomain(evidenceEl.asJsonArray)
        }
        val legacy = v.get("supporting_entry_timestamps")
        if (legacy != null && legacy.isJsonArray) {
            return legacy.asJsonArray.mapNotNull { e ->
                if (e.isJsonPrimitive && e.asJsonPrimitive.isString) {
                    VariabilityEvidence(loggedAt = e.asString, templateId = null, title = null)
                } else {
                    null
                }
            }
        }
        return emptyList()
    }

    private fun parseEvidenceArrayDomain(arr: JsonArray): List<VariabilityEvidence> {
        val out = mutableListOf<VariabilityEvidence>()
        for (e in arr) {
            when {
                e.isJsonPrimitive && e.asJsonPrimitive.isString -> {
                    out.add(VariabilityEvidence(loggedAt = e.asString, templateId = null, title = null))
                }
                e.isJsonObject -> {
                    val o = e.asJsonObject
                    val loggedAt = optString(o, "logged_at") ?: continue
                    val templateId = when (val t = o.get("template_id")) {
                        null -> null
                        is JsonPrimitive -> if (t.isNumber) t.asLong else null
                        else -> null
                    }
                    val title = optString(o, "title")
                    out.add(VariabilityEvidence(loggedAt = loggedAt, templateId = templateId, title = title))
                }
            }
        }
        return out
    }
}

private fun optString(o: JsonObject, key: String): String? =
    o.get(key)?.takeIf { it.isJsonPrimitive && !it.isJsonNull }?.asString
