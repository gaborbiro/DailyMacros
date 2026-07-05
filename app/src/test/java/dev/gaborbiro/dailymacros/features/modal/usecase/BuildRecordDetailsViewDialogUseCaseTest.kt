package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.shared.TemplateUiMapper
import dev.gaborbiro.dailymacros.features.modal.ModalUiMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildRecordDetailsViewDialogUseCaseTest {

    @Test
    fun `view mode is read only without template details`() {
        val build = BuildRecordDetailsViewDialogUseCase(ModalUiMapper(TemplateUiMapper()))
        val record = ModalRecordFixtures.record(1L, ModalRecordFixtures.template(name = "Rice"))
        val dlg = build.execute(record, edit = false)
        assertFalse(dlg.allowEdit)
        assertFalse(dlg.openedFromTemplateDetailsOnly)
        assertEquals(0, dlg.linkedRecordCountForTemplate)
        assertEquals("Rice", dlg.title.text)
        assertEquals("Rice", dlg.pristineSnapshot.title)
        assertEquals(1L, dlg.pristineSnapshot.templateDbId)
    }

    @Test
    fun `edit mode allows editing`() {
        val build = BuildRecordDetailsViewDialogUseCase(ModalUiMapper(TemplateUiMapper()))
        val record = ModalRecordFixtures.record(1L, ModalRecordFixtures.template())
        val dlg = build.execute(record, edit = true)
        assertTrue(dlg.allowEdit)
        assertFalse(dlg.openedFromTemplateDetailsOnly)
    }

    @Test
    fun `template details mode enables add-new-template flow`() {
        val build = BuildRecordDetailsViewDialogUseCase(ModalUiMapper(TemplateUiMapper()))
        val record = ModalRecordFixtures.record(1L, ModalRecordFixtures.template())
        val dlg = build.execute(record, edit = false, templateDetailsMode = true)
        assertTrue(dlg.allowEdit)
        assertTrue(dlg.openedFromTemplateDetailsOnly)
    }
}
