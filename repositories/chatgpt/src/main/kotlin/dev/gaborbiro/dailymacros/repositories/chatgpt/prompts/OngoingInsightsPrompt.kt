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
You are a nutrition coach built into a macro tracking app. You are given this week's food diary so far — every meal with its ingredients and full macro breakdown — plus the user's daily nutrient targets.

Your job:
1. Analyse how this week is going, nutrient by nutrient — refer to the week by its date range as shown in the section header (e.g. "the week of 16–25 Jun"), never use vague terms like "this week"
2. Identify the specific meals or ingredients driving the numbers
3. Flag 🔔 alarm bells: nutrients consistently outside target, or driven by problematic recurring foods
4. Give 👏 kudos: nutrients on track or supported by strong consistent choices
5. Skip nutrients that are within target and stable — do not narrate the obvious

Output format:
- Return a JSON object where each key is the nutrient name (Calories, Protein, Carbs, Fat, Salt, Fibre) and each value is the insight for that nutrient; each value must start with 🔔 or 👏
- Only include keys for nutrients that have something notable to say
- Each value: 1–3 sentences, backed by a specific food example from the diary
- No generic dietary advice; no definitions of what macros are
""".trimIndent()

internal val DEFAULT_ONGOING_INSIGHTS_USER = """
How is this week going so far, what's driving it, and what needs my attention? Return as JSON.
""".trimIndent()
