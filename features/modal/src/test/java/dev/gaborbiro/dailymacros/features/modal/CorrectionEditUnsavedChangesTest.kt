package dev.gaborbiro.dailymacros.features.modal

import androidx.compose.ui.text.input.TextFieldValue
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot
import dev.gaborbiro.dailymacros.features.shared.TemplateUiMapper
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class CorrectionEditUnsavedChangesTest {
    private val mapper = ModalUiMapper(TemplateUiMapper())

    private fun editor(fromCorrection: Boolean = true) = DialogHandle.RecordDetailsDialog.View(
        recordId = 1L,
        templateDbId = 1L,
        variabilityAnchorTemplateDbId = 1L,
        nutrientBreakdown = null,
        compactNutrients = NutrientsUiModel(null, null, null, null, null, null),
        allowEdit = true,
        isEditing = true,
        editingFromCorrection = fromCorrection,
        titleHint = "Title",
        title = TextFieldValue("Rice"),
        description = TextFieldValue("Brown rice\n"),
        imageFilenames = listOf("meal.jpg"),
        pristineSnapshot = RecordDetailsPristineSnapshot(1L, "Rice", "Brown rice", listOf("meal.jpg")),
        timestamp = ZonedDateTime.now(),
    )

    @Test
    fun `opening the description shortcut alone does not require a discard confirmation`() {
        assertFalse(mapper.hasUnsavedEdits(editor()))
    }

    @Test
    fun `description rewrites title changes and photo edits still require confirmation`() {
        val original = editor()
        assertTrue(mapper.hasUnsavedEdits(original.copy(description = TextFieldValue("Half the rice"))))
        assertTrue(mapper.hasUnsavedEdits(original.copy(title = TextFieldValue("Curry"))))
        assertTrue(mapper.hasUnsavedEdits(original.copy(imageFilenames = emptyList())))
        assertFalse(mapper.hasUnsavedEdits(original.copy(description = TextFieldValue("Brown rice"))))
    }

    @Test
    fun `ordinary edit meal retains its existing exact text comparison`() {
        assertTrue(mapper.hasUnsavedEdits(editor(fromCorrection = false)))
    }
}
