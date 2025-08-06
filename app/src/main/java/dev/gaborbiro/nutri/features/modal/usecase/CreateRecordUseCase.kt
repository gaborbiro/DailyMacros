package dev.gaborbiro.nutri.features.modal.usecase

import androidx.annotation.UiThread
import dev.gaborbiro.nutri.data.records.domain.RecordsRepository
import dev.gaborbiro.nutri.data.records.domain.model.RecordToSave
import dev.gaborbiro.nutri.data.records.domain.model.TemplateToSave
import dev.gaborbiro.nutri.features.common.BaseUseCase
import java.time.LocalDateTime

class CreateRecordUseCase(
    private val repository: RecordsRepository,
) : BaseUseCase() {

    @UiThread
    suspend fun execute(
        image: String?,
        title: String,
        description: String
    ) {
        val record = RecordToSave(
            timestamp = LocalDateTime.now(),
            template = TemplateToSave(
                image = image,
                name = title,
                description = description,
            ),
        )
        repository.saveRecord(record)
    }
}
