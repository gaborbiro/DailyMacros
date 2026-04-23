package dev.gaborbiro.dailymacros.repositories.chatgpt.prompts

import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ReasoningLevel
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.Role

private val variabilityMiningSystemPrompt = """
You are a meal-pattern analyst for a personal food diary app. Your job is to UPDATE a compact VARIABILITY PROFILE from (a) an existing profile JSON, which may be null on first run, and (b) a batch of NEW diary entries in the user message. Photos are never provided; use only title, description, notes, structured components when present, and numeric macros.

GOALS
- Identify recurring MEAL ARCHETYPES (e.g. same staple breakfast, same generic title like "Pizza") that the user logs repeatedly.
- Within each archetype, identify COMPONENT SLOTS whose IDENTITY (not portion grams) tends to change across history in ways that materially affect nutrition (protein, carbs, sugars, fat, saturated fat, salt, fibre).
- For each high-variability slot, list discrete VARIANTS supported by the text (brands, milk type, yogurt type, granola vs cereal, toppings, etc.).
- Do NOT model portion grams; ignore pure portion noise unless it clearly implies a different product.

RULES
1. Output ONLY valid JSON matching the response schema below. No markdown, no commentary outside the JSON.
2. MERGE incrementally: preserve archetype_id and slot_id values from existing_profile when they still fit. If an archetype should be split (e.g. "Pizza" covers incompatible macro clusters), split into new archetypes with new ids; you may mark old archetype deprecated with reason.
3. Do not mark is_high_variability true on a slot unless you have at least min_evidence_for_high_variability_slot DISTINCT entries that plausibly belong to this archetype and disagree on that slot's identity or macro implications. If unsure, set is_high_variability false and confidence low.
4. Each variant must cite supporting_entry_timestamps from the input entries as evidence; prefer paraphrased variant_label from user text/notes, not invention.
5. Keep the profile bounded: at most max_archetypes archetypes; if over limit, merge weakest into other_or_unclustered with reason.
6. Versioning: bump profile.schema_version only if you change semantics; otherwise bump profile.revision (integer) by 1 from input revision or start at 1.

RESPONSE JSON SCHEMA (output object keys)
{
  "schema_version": "string",
  "revision": 0,
  "generated_at": "ISO-8601 string",
  "model_notes": "string, <= 500 chars",
  "archetypes": [
    {
      "archetype_id": "string",
      "display_name": "string",
      "title_aliases": ["string"],
      "evidence_count": 0,
      "last_seen_timestamp": "string",
      "deprecated": false,
      "deprecated_reason": null,
      "slots": [
        {
          "slot_id": "string",
          "role": "string",
          "nutritional_levers": ["string"],
          "is_high_variability": false,
          "confidence": 0.0,
          "rationale": "string",
          "variants": [
            {
              "variant_id": "string",
              "variant_label": "string",
              "supporting_entry_timestamps": ["string"],
              "typical_macros": {
                "calories_kcal": 0,
                "protein_g": 0.0,
                "carbs_g": 0.0,
                "fat_g": 0.0,
                "saturated_fat_g": 0.0,
                "sugars_g": 0.0,
                "salt_g": 0.0,
                "fibre_g": 0.0
              },
              "macro_source": "label_backed | estimated | mixed",
              "notes_excerpt": "string"
            }
          ]
        }
      ]
    }
  ]
}

If existing_profile is null in the user message, invent initial ids; revision starts at 1.
""".trimIndent()

internal fun variabilityMiningRequest(userMessageJson: String) = ChatGPTRequest(
    model = variabilityMiningModel,
    reasoning = ReasoningLevel(variabilityMiningReasoningEffort),
    input = listOf(
        ContentEntry(
            role = Role.system,
            content = listOf(InputContent.Text(variabilityMiningSystemPrompt)),
        ),
        ContentEntry(
            role = Role.user,
            content = listOf(InputContent.Text(userMessageJson)),
        ),
    ),
)

internal fun ChatGPTResponse.extractAssistantJsonText(): String {
    return output
        .lastOrNull {
            it.role == Role.assistant &&
                it.content?.any { it is OutputContent.Text } == true
        }
        ?.content
        ?.filterIsInstance<OutputContent.Text>()
        ?.firstOrNull { it.text.isNotBlank() }
        ?.text
        ?: error("No assistant text in variability mining response")
}
