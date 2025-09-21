package dev.gaborbiro.dailymacros.repo.requestStatus.domain.model

data class RequestStatus(
    val templateId: Long,
    val isPending: Boolean,
    val startedAt: Long = System.currentTimeMillis(),
    val message: String? = null,
)
