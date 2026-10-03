package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.ImageInputType
import dev.gaborbiro.dailymacros.features.modal.model.ModalUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZonedDateTime

class ModalScreenNameTest {

    @Test
    fun `no root dialog means no screen`() {
        assertNull(ModalUiState(overlayDialog = DialogHandle.InfoDialog("x")).toModalScreen())
    }

    @Test
    fun `root dialog alone is named under modal`() {
        assertEquals(
            ModalScreen("modal/camera"),
            ModalUiState(rootDialog = DialogHandle.ImageInput(ImageInputType.Camera)).toModalScreen(),
        )
    }

    @Test
    fun `overlay dialog is appended to the root`() {
        val state = ModalUiState(
            rootDialog = DialogHandle.ViewImageDialog(title = "t", imageFilenames = listOf("a.jpg")),
            overlayDialog = DialogHandle.EditTimestampDialog(recordId = 1L, timestamp = ZonedDateTime.now()),
        )
        assertEquals(ModalScreen("modal/image_viewer/edit_timestamp"), state.toModalScreen())
    }
}
