package dev.gaborbiro.dailymacros.features.shared

import androidx.annotation.UiThread
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import java.time.ZonedDateTime

import javax.inject.Inject

class CreateRecordFromTemplateUseCase @Inject constructor(
    private val recordsRepository: RecordsRepository,
) {

    /**
     * [timestamp] must be captured by the caller when the user started assembling this record
     * (e.g. when the create-record screen was opened), not when this use case runs — the two can
     * be far apart if the app was backgrounded mid-creation.
     */
    @UiThread
    suspend fun execute(
        templateId: Long,
        timestamp: ZonedDateTime,
    ): Long {
        return recordsRepository.saveRecord(templateId, timestamp)
    }
}
