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
fun EditTargetConfirmationDialog(
    dialogState: DialogState.EditTargetConfirmationDialog,
    onEditTargetConfirmed: (ModalScreenViewModel.Companion.EditTarget) -> Unit,
    onDialogDismissed: () -> Unit,
) {
    TargetConfirmationDialog(
        count = dialogState.count,
        onConfirmed = onEditTargetConfirmed,
        onDialogDismissed = onDialogDismissed,
    )
}


@Composable
fun EditImageTargetConfirmationDialog(
    dialogState: DialogState.EditImageTargetConfirmationDialog,
    onEditImageTargetConfirmed: (ModalScreenViewModel.Companion.EditTarget) -> Unit,
    onDialogDismissed: () -> Unit,
) {
    TargetConfirmationDialog(
        count = dialogState.count,
        onConfirmed = onEditImageTargetConfirmed,
        onDialogDismissed = onDialogDismissed,
    )
}

@Composable
fun TargetConfirmationDialog(
    count: Int,
    onConfirmed: (ModalScreenViewModel.Companion.EditTarget) -> Unit,
    onDialogDismissed: () -> Unit,
) {
    Dialog(onDismissRequest = onDialogDismissed) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 4.dp,
        ) {
            EditTargetConfirmationDialogContent(
                count = count,
                onSubmit = { target ->
                    val vmTarget = when (target) {
                        EditTarget.RECORD -> ModalScreenViewModel.Companion.EditTarget.RECORD
                        EditTarget.TEMPLATE -> ModalScreenViewModel.Companion.EditTarget.TEMPLATE
                    }
                    onConfirmed(vmTarget)
                },
                onCancel = onDialogDismissed,
            )
        }
    }
}
