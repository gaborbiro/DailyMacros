package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

sealed class PromptSegment {
    data class Locked(val text: String) : PromptSegment()
    data class Editable(
        val id: String,
        val label: String,
        val defaultText: String,
        val hint: String = "",
        val singleLine: Boolean = false,
    ) : PromptSegment()
}
