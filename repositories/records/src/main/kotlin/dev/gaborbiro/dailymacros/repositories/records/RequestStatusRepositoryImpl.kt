package dev.gaborbiro.dailymacros.repositories.records

import dev.gaborbiro.dailymacros.data.db.RequestStatusDAO
import dev.gaborbiro.dailymacros.data.db.model.entity.RequestStatusEntity
import dev.gaborbiro.dailymacros.repositories.records.domain.RequestStatusRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.RequestStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import kotlin.time.Duration.Companion.seconds

class RequestStatusRepositoryImpl(
    private val requestStatusDAO: RequestStatusDAO,
    private val clock: Clock = Clock.systemDefaultZone(),
) : RequestStatusRepository {

    companion object {
        private val REQUEST_TIMEOUT = 60.seconds
    }

    override suspend fun markAsPending(templateId: Long) {
        requestStatusDAO.insertOrUpdate(
            RequestStatusEntity(templateId)
        )
    }

    override suspend fun isPending(templateId: Long): Boolean {
        return requestStatusDAO.getByTemplate(templateId)?.status == RequestStatusEntity.Status.PENDING
    }

    override suspend fun observeAll(): Flow<List<RequestStatus>> {
        return requestStatusDAO.observeAll()
            .map {
                it.map(::map)
            }
    }

    override suspend fun getAll(): List<RequestStatus> {
        return requestStatusDAO.getAll()
            .map(::map)
    }

    private fun map(requestStatus: RequestStatusEntity): RequestStatus {
        return RequestStatus(
            templateId = requestStatus.templateId,
            isPending = requestStatus.status == RequestStatusEntity.Status.PENDING,
            startedAt = requestStatus.startedAt,
            message = requestStatus.message,
        )
    }

    override suspend fun unmark(templateId: Long) {
        requestStatusDAO.deleteByTemplate(templateId)
    }

    override suspend fun deleteStale() {
        requestStatusDAO.deleteStale(
            status = RequestStatusEntity.Status.PENDING,
            cutoff = clock.millis() - REQUEST_TIMEOUT.inWholeMilliseconds,
        )
    }
}