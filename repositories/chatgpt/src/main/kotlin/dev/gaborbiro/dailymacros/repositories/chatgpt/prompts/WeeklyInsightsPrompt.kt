package dev.gaborbiro.dailymacros.repositories.chatgpt.prompts

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.WeeklyInsightsRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.WeeklyInsightsResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.FormatType
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ReasoningLevel
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.Role
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.TextOptions


internal val DEFAULT_WEEKLY_INSIGHTS_SYSTEM = """
You are a nutrition coach built into a macro tracking app. 
You are given two weeks of food diary entries — every meal with its ingredients and full macro breakdown — plus the user's daily nutrient targets.

GUIDELINES:
1. Compare the two weeks in the diary, nutrient by nutrient — always refer to each week by its date range as shown in the section header (e.g. “the week of 16–22 Jun”), never use the terms “this week” or “last week”
2. Identify the specific meals or ingredients responsible for notable changes
3. Flag with 👀: nutrients that are worsening, consistently outside target, or driven by problematic recurring foods
4. Give 👏 kudos: nutrients that improved, hit target, or are supported by strong consistent choices
5. Skip nutrients that are within target and stable — do not narrate the obvious
6. No generic dietary advice; no definitions of what macros are

OUTPUT RULES:
- Return a JSON object where each key is the nutrient name (Calories, Protein, Carbs, Fat, Salt, Fibre) and each value is the insight for that nutrient; each value must start with 👀 or 👏
- Only include nutrient keys for nutrients that have something notable to say
- Each nutrient value: 1–3 sentences, backed by a specific food example from the diary
- Always include a "week_assessment" key: a focused assessment of the most recent week only (the first week in the diary), from the perspective of the user's personal health goal stated in the user message; evaluate the nutrients most relevant to that goal, call out specific foods that helped or hurt, and state whether the week was broadly positive or concerning; 2–4 sentences; no emoji

LANGUAGE RULES:
- All output (title or error message) MUST be in {phone_language}.
- Never switch output language based on packaging language.
- If packaging text is not in {phone_language}, translate output into {phone_language}.
""".trimIndent()

internal val DEFAULT_WEEKLY_INSIGHTS_USER = """
TASK: WEEK-ON-WEEK INSIGHTS
What changed week-over-week, what drove it, and what needs my attention?
My personal health goal: cholesterol control.
""".trimIndent()

internal fun WeeklyInsightsRequest.toApiModel() = ChatGPTRequest(
    model = customisations.systemPrompt(SEG_WEEKLY_INSIGHTS_MODEL, weeklyInsightsModel),
    reasoning = ReasoningLevel(customisations.systemPrompt(SEG_WEEKLY_INSIGHTS_REASONING_EFFORT, weeklyInsightsReasoningEffort)),
    text = TextOptions(FormatType("json_object")),
    input = listOf(
        ContentEntry(
            role = Role.system,
            content = listOf(
                InputContent.Text(
                    customisations.systemPrompt(SEG_WEEKLY_INSIGHTS_SYSTEM, DEFAULT_WEEKLY_INSIGHTS_SYSTEM)
                        .replace("{phone_language}", this.phoneLanguage)
                )
            ),
        ),
        ContentEntry(
            role = Role.user,
            content = listOf(InputContent.Text(diary)),
        ),
        ContentEntry(
            role = Role.user,
            content = listOf(
                InputContent.Text(
                    customisations.systemPrompt(SEG_WEEKLY_INSIGHTS_USER, DEFAULT_WEEKLY_INSIGHTS_USER)
                )
            ),
        ),
    )
)

private const val KEY_WEEK_ASSESSMENT = "week_assessment"

internal fun ChatGPTResponse.toWeeklyInsightsResponse(): WeeklyInsightsResult {
    return try {
        val obj = org.json.JSONObject(this.resultJson())
        val weekAssessment = if (obj.has(KEY_WEEK_ASSESSMENT)) obj.getString(KEY_WEEK_ASSESSMENT) else null
        val insights = obj.keys().asSequence()
            .filter { it != KEY_WEEK_ASSESSMENT }
            .associateWith { obj.getString(it) }
        WeeklyInsightsResult(insights = insights, weekAssessment = weekAssessment)
    } catch (_: Exception) {
        WeeklyInsightsResult(insights = emptyMap())
    }
}
