package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplyQuickPickOverrideAndReloadWidgetUseCaseTest {

    @Test
    fun `delegates to repository`() = runBlocking {
        var seenTemplate: Long? = null
        var seenOverride: Template.QuickPickOverride? = null
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun addQuickPickOverride(templateId: Long, type: Template.QuickPickOverride) {
                seenTemplate = templateId
                seenOverride = type
            }
        }
        ApplyQuickPickOverrideAndReloadWidgetUseCase(repo).execute(3L, Template.QuickPickOverride.INCLUDE)
        assertEquals(3L, seenTemplate)
        assertEquals(Template.QuickPickOverride.INCLUDE, seenOverride)
    }
}
