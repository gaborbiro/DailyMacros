package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.features.modal.ModalScreenViewModel

@Composable
internal fun EditTargetConfirmationDialog(
    dialogState: DialogState.EditTargetConfirmationDialog,
    onEditTargetConfirmed: (ModalScreenViewModel.Companion.ChangeImagesTarget) -> Unit,
    onDismissRequested: () -> Unit,
) {
    TargetConfirmationDialog(
        count = dialogState.count,
        onConfirmed = onEditTargetConfirmed,
        onDismissRequested = onDismissRequested,
    )
}

@Composable
internal fun TargetConfirmationDialog(
    count: Int,
    onConfirmed: (ModalScreenViewModel.Companion.ChangeImagesTarget) -> Unit,
    onDismissRequested: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequested) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 4.dp,
        ) {
            EditTargetConfirmationDialogContent(
                count = count,
                onSubmit = { target ->
                    val vmTarget = when (target) {
                        EditTarget.RECORD -> ModalScreenViewModel.Companion.ChangeImagesTarget.RECORD
                        EditTarget.TEMPLATE -> ModalScreenViewModel.Companion.ChangeImagesTarget.TEMPLATE
                    }
                    onConfirmed(vmTarget)
                },
                onCancel = onDismissRequested,
            )
        }
    }
}
