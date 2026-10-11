package dev.gaborbiro.dailymacros.repositories.records.domain.model

data class RequestStatus(
    val templateId: Long,
    val isPending: Boolean,
    val startedAt: Long,
    val message: String? = null,
)
