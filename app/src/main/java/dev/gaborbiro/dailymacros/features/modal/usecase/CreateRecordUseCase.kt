package dev.gaborbiro.dailymacros.features.modal.usecase

import androidx.annotation.UiThread
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repo.records.domain.model.RecordToSave
import dev.gaborbiro.dailymacros.repo.records.domain.model.TemplateToSave
import java.time.ZonedDateTime

internal class CreateRecordUseCase(
    private val recordsRepository: RecordsRepository,
) {

    @UiThread
    suspend fun execute(
        images: List<String>,
        title: String,
        description: String,
    ): Long {
        val record = RecordToSave(
            timestamp = ZonedDateTime.now(),
            templateToSave = TemplateToSave(
                images = images,
                name = title,
                description = description,
            ),
        )
        return recordsRepository.saveRecord(record)
    }
}
