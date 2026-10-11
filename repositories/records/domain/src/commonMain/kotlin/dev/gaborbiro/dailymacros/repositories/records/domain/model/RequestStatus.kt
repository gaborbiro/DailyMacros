package dev.gaborbiro.dailymacros.repositories.records.domain.model

import kotlin.time.Clock

data class RequestStatus(
    val templateId: Long,
    val isPending: Boolean,
    val startedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val message: String? = null,
)
