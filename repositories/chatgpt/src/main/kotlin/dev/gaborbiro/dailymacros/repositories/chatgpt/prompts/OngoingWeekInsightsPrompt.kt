package dev.gaborbiro.dailymacros.repositories.chatgpt.prompts

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.OngoingWeekInsightsRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.OngoingWeekInsightsResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ReasoningLevel
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.Role


internal val DEFAULT_ONGOING_WEEK_INSIGHTS_SYSTEM = """
You are a nutrition coach built into a macro tracking app. Write a weekly progress check-in for the user — they are reviewing their week so far in the app.
You are given this week's food diary so far — every meal with its ingredients and full macro breakdown — plus the user's daily nutrient targets.

GUIDELINES:
1. Call out stacking: if multiple days are already off in the same direction, name it — consecutive bad days compound and are harder to recover from
2. Give a recovery angle where relevant: is the damage already done, or is there still room to course-correct?
3. Use a direct, honest tone — "three high-salt days in a row" beats vague concern
4. Back every observation with a specific meal or ingredient from the diary
5. No generic dietary advice; no definitions of what macros are

OUTPUT RULES:
Use this JSON format:
{
  "message": ""
}

In case there is not enough information to gain any meaningful insights:
{
  "error": "<one short sentence explaining clearly what went wrong>"
}

LANGUAGE RULES:
- All output (title or error message) MUST be in {phone_language}.
- Never switch output language based on packaging language.
- If packaging text is not in {phone_language}, translate output into {phone_language}.
""".trimIndent()

internal val DEFAULT_ONGOING_WEEK_INSIGHTS_USER = """
TASK: ONGOING WEEK INSIGHTS
Write a weekly progress check-in based on the diary above.
""".trimIndent()

internal fun OngoingWeekInsightsRequest.toApiModel() = ChatGPTRequest(
    model = customizations.systemPrompt(SEG_ONGOING_WEEK_INSIGHTS_MODEL, ongoingWeekInsightsModel),
    reasoning = ReasoningLevel(customizations.systemPrompt(SEG_ONGOING_WEEK_INSIGHTS_REASONING_EFFORT, ongoingWeekInsightsReasoningEffort)),
    input = listOf(
        ContentEntry(
            role = Role.system,
            content = listOf(
                InputContent.Text(
                    customizations
                        .systemPrompt(SEG_ONGOING_WEEK_INSIGHTS_SYSTEM, DEFAULT_ONGOING_WEEK_INSIGHTS_SYSTEM)
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
                    customizations.systemPrompt(SEG_ONGOING_WEEK_INSIGHTS_USER, DEFAULT_ONGOING_WEEK_INSIGHTS_USER)
                )
            ),
        ),
    )
)

internal fun ChatGPTResponse.toOngoingInsightsResult(): OngoingWeekInsightsResult {
    class InsightsResponse(
        @SerializedName("message") val message: String?,
        @SerializedName("error") val error: String?,
    )

    val gson = GsonBuilder().create()
    val response = gson.fromJson(this.resultJson(), InsightsResponse::class.java)
    return OngoingWeekInsightsResult(
        message = response.message.takeIf { it.isNullOrBlank().not() },
        error = response.error.takeIf { it.isNullOrBlank().not() },
    )
}
