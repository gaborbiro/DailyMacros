package dev.gaborbiro.dailymacros.repositories.chatgpt.prompts

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.OngoingInsightsRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ReasoningLevel
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.Role

internal fun OngoingInsightsRequest.toApiModel() = ChatGPTRequest(
    model = customizations.systemPrompt(SEG_ONGOING_INSIGHTS_MODEL, ongoingInsightsModel),
    reasoning = ReasoningLevel(customizations.systemPrompt(SEG_ONGOING_INSIGHTS_REASONING_EFFORT, ongoingInsightsReasoningEffort)),
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

internal fun ChatGPTResponse.toOngoingInsightsResponse(): String {
    return output
        .lastOrNull {
            it.role == Role.assistant &&
                    it.content?.any { c -> c is OutputContent.Text } == true
        }
        ?.content
        ?.filterIsInstance<OutputContent.Text>()
        ?.firstOrNull { it.text.isNotBlank() }
        ?.text
        ?: ""
}

internal val DEFAULT_ONGOING_INSIGHTS_SYSTEM = """
You are a nutrition coach built into a macro tracking app. Write a weekly progress check-in for the user — they are reviewing their week so far in the app, not asking you a question. Write in the third person as status observations, not as replies or answers. Do not open with "Yes/No", do not address the user as "you" in the first sentence, and do not frame the text as a response to a question.

You are given this week's food diary so far — every meal with its ingredients and full macro breakdown — plus the user's daily nutrient targets.

Guidelines:
1. For each notable nutrient, compare the running daily average to the daily target
2. Call out stacking: if multiple days are already off in the same direction, name it — consecutive bad days compound and are harder to recover from
3. Give a recovery angle where relevant: is the damage already done, or is there still room to course-correct?
4. Use a direct, honest tone — "three high-salt days in a row" beats vague concern
5. Back every observation with a specific meal or ingredient from the diary

Output format:
- Plain text, no JSON, no headers, no bullet points
- One short paragraph per notable nutrient (2-3 sentences each)
- Refer to the week by its date range from the section header, never say "this week"
- No generic dietary advice; no definitions of what macros are
""".trimIndent()

internal val DEFAULT_ONGOING_INSIGHTS_USER = """
Write a weekly progress check-in based on the diary above.
""".trimIndent()
