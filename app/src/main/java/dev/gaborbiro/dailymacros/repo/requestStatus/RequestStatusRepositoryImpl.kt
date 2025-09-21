package dev.gaborbiro.dailymacros.repo.requestStatus

import dev.gaborbiro.dailymacros.data.db.RequestStatusDAO
import dev.gaborbiro.dailymacros.data.db.model.entity.RequestStatusEntity
import dev.gaborbiro.dailymacros.repo.requestStatus.domain.RequestStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import kotlin.time.Duration.Companion.seconds
import dev.gaborbiro.dailymacros.data.db.model.entity.RequestStatus as RequestStatusDB
import dev.gaborbiro.dailymacros.repo.requestStatus.domain.model.RequestStatus as RequestStatusDomain

class RequestStatusRepositoryImpl(
    private val requestStatusDAO: RequestStatusDAO,
    private val clock: Clock = Clock.systemUTC(),
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
        return requestStatusDAO.getByTemplate(templateId)?.status == RequestStatusDB.PENDING
    }

    override suspend fun observeAll(): Flow<List<RequestStatusDomain>> {
        return requestStatusDAO.observeAll()
            .map {
                it.map(::map)
            }
    }

    override suspend fun getAll(): List<RequestStatusDomain> {
        return requestStatusDAO.getAll()
            .map(::map)
    }

    private fun map(requestStatus: RequestStatusEntity): RequestStatusDomain {
        return RequestStatusDomain(
            templateId = requestStatus.templateId,
            isPending = requestStatus.status == RequestStatusDB.PENDING,
            startedAt = requestStatus.startedAt,
            message = requestStatus.message,
        )
    }

    override suspend fun unmark(templateId: Long) {
        requestStatusDAO.deleteByTemplate(templateId)
    }

    override suspend fun deleteStale() {
        requestStatusDAO.deleteStale(
            status = RequestStatusDB.PENDING,
            cutoff = clock.millis() - REQUEST_TIMEOUT.inWholeMilliseconds,
        )
    }
}
