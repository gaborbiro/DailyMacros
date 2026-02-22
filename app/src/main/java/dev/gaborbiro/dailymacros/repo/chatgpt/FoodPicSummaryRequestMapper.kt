package dev.gaborbiro.dailymacros.repo.chatgpt

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.PhotoAnalysisRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.PhotoAnalysisResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.Role

internal fun PhotoAnalysisRequest.toApiModel(): ChatGPTRequest {
    return ChatGPTRequest(
        model = model,
        input = listOf(
            ContentEntry(
                role = Role.system,
                content = listOf(InputContent.Text(SHARED_SYSTEM_PROMPT)),
            ),
            ContentEntry(
                role = Role.user,
                content = base64Images.map {
                    InputContent.Image(it)
                }
            ),
            ContentEntry(
                role = Role.user,
                content = listOf(
                    InputContent.Text(
                        """
TASK: RECOGNITION

Return structured food breakdown suitable for later macro estimation.

Output format:
{
  "components": [
    {
      "name": "",
      "estimatedAmount": "",
      "confidence": "high|medium|low"
    }
  ],
  "title": "",
  "description": ""
}
If food cannot be determined:
{
  "error": ""
}
"""
                    )
                )
            )
        )
    )
}

private val gson = GsonBuilder().create()

internal fun ChatGPTResponse.toPhotoAnalysisResponse(): PhotoAnalysisResponse {
    val resultJson: String? = this.output
        .lastOrNull {
            it.role == Role.assistant &&
                    it.content?.any { it is OutputContent.Text } == true
        }
        ?.content
        ?.filterIsInstance<OutputContent.Text>()
        ?.firstOrNull {
            it.text.isNotBlank()
        }
        ?.text

    class Component(
        @SerializedName("name") val name: String?,
        @SerializedName("estimatedAmount") val estimatedAmount: String?,
        @SerializedName("confidence") val confidence: String?,
    )

    class AnalysisResult(
        @SerializedName("title") val title: String?,
        @SerializedName("description") val description: String?,
        @SerializedName("components") val components: List<Component>,
        @SerializedName("error") val error: String?,
    )

    return resultJson
        ?.let {
            val analysisResult = gson.fromJson(resultJson, AnalysisResult::class.java)
            if (analysisResult.error != null) {
                throw ChatGPTApiError.GenericApiError(analysisResult.error)
            }
            val componentStr = analysisResult.components.joinToString("\n") { component ->
                val confidence = when (component.confidence) {
                    "medium" -> "?"
                    "low" -> "??"
                    else -> null
                }
                "${component.estimatedAmount} ${component.name} ${confidence?.let { "($it)" }}"
            }

            PhotoAnalysisResponse(
                title = analysisResult.title,
                description = "${analysisResult.description}\n$componentStr",
            )
        }
        ?: PhotoAnalysisResponse(
            title = null,
            description = null,
        )
}
