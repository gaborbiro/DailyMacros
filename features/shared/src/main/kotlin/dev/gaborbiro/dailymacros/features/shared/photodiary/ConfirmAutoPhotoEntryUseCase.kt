package dev.gaborbiro.dailymacros.features.shared.photodiary

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.features.shared.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.shared.NutrientAnalysisWorker
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import javax.inject.Inject

class ConfirmAutoPhotoEntryUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val recordsRepository: RecordsRepository,
    private val createRecordFromTemplateUseCase: CreateRecordFromTemplateUseCase,
) {

    suspend fun execute(imageFilename: String, recognisedTitle: String) {
        val templateId = recordsRepository.saveTemplate(
            TemplateToSave(
                imageFilenames = listOf(imageFilename),
                name = recognisedTitle,
                description = "",
                parentTemplateId = null,
            )
        )
        val recordId = createRecordFromTemplateUseCase.execute(templateId)
        NutrientAnalysisWorker.setWorkRequest(appContext, recordId, force = true)
    }
}
