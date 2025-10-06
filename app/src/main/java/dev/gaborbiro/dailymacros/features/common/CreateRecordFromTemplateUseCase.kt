package dev.gaborbiro.dailymacros.features.common

import androidx.annotation.UiThread
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import java.time.ZoneId
import java.time.ZonedDateTime

internal class CreateRecordFromTemplateUseCase(
    private val recordsRepository: RecordsRepository,
) {

    @UiThread
    suspend fun execute(
        templateId: Long,
    ): Long {
        val timestamp = ZonedDateTime.now(ZoneId.systemDefault())
//        val timestamp = ZonedDateTime.of(2025, 9, 29, 2, 22, 0, 0, ZoneId.of("Asia/Kamchatka"))
        return recordsRepository.saveRecord(templateId, timestamp)
    }
}
