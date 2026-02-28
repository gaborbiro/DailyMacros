package dev.gaborbiro.dailymacros.repositories.records.domain

import dev.gaborbiro.dailymacros.repositories.records.domain.model.RequestStatus
import kotlinx.coroutines.flow.Flow

interface RequestStatusRepository {

    /**
     * Mark the specified template having a pending/loading request.
     */
    suspend fun markAsPending(templateId: Long)

    suspend fun isPending(templateId: Long): Boolean

    suspend fun observeAll(): Flow<List<RequestStatus>>

    suspend fun getAll(): List<RequestStatus>

    suspend fun unmark(templateId: Long)

    suspend fun deleteStale()
}
