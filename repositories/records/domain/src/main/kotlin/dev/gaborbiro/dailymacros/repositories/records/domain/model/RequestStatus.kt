package dev.gaborbiro.dailymacros.repositories.records.domain.model

data class RequestStatus(
    val templateId: Long,
    val isPending: Boolean,
    val startedAt: Long = System.currentTimeMillis(),
    val message: String? = null,
)
