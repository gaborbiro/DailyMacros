package dev.gaborbiro.dailymacros.repositories.chatgpt.prompts

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.OngoingInsightsRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.FormatType
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ReasoningLevel
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.Role
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.TextOptions

internal fun OngoingInsightsRequest.toApiModel() = ChatGPTRequest(
    model = customizations.systemPrompt(SEG_ONGOING_INSIGHTS_MODEL, ongoingInsightsModel),
    reasoning = ReasoningLevel(customizations.systemPrompt(SEG_ONGOING_INSIGHTS_REASONING_EFFORT, ongoingInsightsReasoningEffort)),
    text = TextOptions(FormatType("json_object")),
    input = listOf(
        ContentEntry(
            role = Role.system,
            content = listOf(InputContent.Text(
                customizations.systemPrompt(SEG_ONGOING_INSIGHTS_SYSTEM, DEFAULT_ONGOING_INSIGHTS_SYSTEM)
            )),
        ),
        ContentEntry(
            role = Role.user,
            content = listOf(InputContent.Text(diary)),
        ),
        ContentEntry(
            role = Role.user,
            content = listOf(InputContent.Text(
                customizations.systemPrompt(SEG_ONGOING_INSIGHTS_USER, DEFAULT_ONGOING_INSIGHTS_USER)
            )),
        ),
    )
)

internal fun ChatGPTResponse.toOngoingInsightsResponse(): Map<String, String> {
    val json = output
        .lastOrNull {
            it.role == Role.assistant &&
                    it.content?.any { c -> c is OutputContent.Text } == true
        }
        ?.content
        ?.filterIsInstance<OutputContent.Text>()
        ?.firstOrNull { it.text.isNotBlank() }
        ?.text
        ?: return emptyMap()
    return try {
        val obj = org.json.JSONObject(json)
        obj.keys().asSequence().associateWith { obj.getString(it) }
    } catch (_: Exception) {
        emptyMap()
    }
}

internal val DEFAULT_ONGOING_INSIGHTS_SYSTEM = """
You are a nutrition coach built into a macro tracking app. You are given this week’s food diary so far — every meal with its ingredients and full macro breakdown — plus the user’s daily nutrient targets.

The core question to answer: “Am I messing up my week, and can I still recover?”

For each notable nutrient:
1. Compare the running daily average so far to the daily target
2. Call out stacking: if multiple days are already off in the same direction, name it — consecutive bad days compound and are harder to recover from
3. Give a recovery angle where relevant: is the damage already done, or is there still room to course-correct?
4. Use a direct, honest tone — “you’ve had three high-salt days in a row” beats vague concern
5. Back every observation with a specific meal or ingredient from the diary

Output format:
- Return a JSON object where each key is the nutrient name (e.g. Calories, Protein, Fat, Saturated Fat, Carbs, Salt, Fibre) and each value is the insight; each value must start with 🔔 or 👏
- Only include keys for nutrients that have something notable to say
- Each value: 2–3 sentences max
- Refer to the week by its date range from the section header, never say “this week”
- No generic dietary advice; no definitions of what macros are
""".trimIndent()

internal val DEFAULT_ONGOING_INSIGHTS_USER = """
Am I messing up my week? What’s stacking up, and can I still fix it? Return as JSON.
""".trimIndent()
