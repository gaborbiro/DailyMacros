package dev.gaborbiro.dailymacros.features.overview.model

import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.features.common.model.BaseListItem

data class OverviewViewState(
    val list: List<BaseListItem> = emptyList(),
    val showUndoDeleteSnackbar: Boolean = false,
    val recordToUndelete: Record? = null,
    val dialog: DialogState? = null,
)

sealed class DialogState {
    data class ConfirmDestructiveChangeDialog(
        val editState: EditState,
    ) : DialogState()
}

sealed class EditState {
    data class ChangeImage(val recordId: Long) : EditState()
    data class RemoveImage(val templateId: Long) : EditState()
}
