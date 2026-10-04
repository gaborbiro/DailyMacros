package dev.gaborbiro.dailymacros.features.modal

import android.os.Bundle
import androidx.core.os.bundleOf
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.ImageInputType
import dev.gaborbiro.dailymacros.features.modal.model.ModalUiState

/**
 * Analytics screen view for whatever ModalActivity is showing. Its screens are dialogs, not
 * Activities or nav destinations, so nothing tracks them automatically. Names are spelled out
 * (not class names) because R8 renames classes in release builds.
 */
internal data class ModalScreen(
    val name: String,
    val recordId: Long? = null,
    val templateId: Long? = null,
) {
    val args: Bundle?
        get() = recordId?.let { bundleOf("recordId" to it, "templateId" to templateId) }
}

internal fun ModalUiState.toModalScreen(): ModalScreen? {
    val root = rootDialog ?: return null
    val name = listOfNotNull(root, overlayDialog)
        .joinToString(separator = "/", prefix = "modal/") { it.screenName() }
    val view = root as? DialogHandle.RecordDetailsDialog.View
    return ModalScreen(name, recordId = view?.recordId, templateId = view?.templateDbId)
}

private fun DialogHandle.screenName(): String = when (this) {
    is DialogHandle.RecordDetailsDialog.Edit -> "new_record"
    is DialogHandle.RecordDetailsDialog.View -> if (isEditing) "record_edit" else "record_view"
    is DialogHandle.ViewImageDialog -> "image_viewer"
    is DialogHandle.ImageInput -> when (type) {
        ImageInputType.Camera -> "camera"
        ImageInputType.BrowseImages -> "photo_picker"
    }
    is DialogHandle.ConfirmSwitchTemplateDialog -> "confirm_switch_template"
    is DialogHandle.InfoDialog -> "info"
    is DialogHandle.QuickPickWidgetConfirmDialog -> "quick_pick_widget_confirm"
    is DialogHandle.EditTimestampDialog -> "edit_timestamp"
}
