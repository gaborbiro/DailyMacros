package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.gaborbiro.dailymacros.features.modal.R
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle

@Composable
internal fun QuickPickWidgetConfirmDialog(
    dialogHandle: DialogHandle.QuickPickWidgetConfirmDialog,
    onLogAgainTapped: () -> Unit,
    onOpenDetailsTapped: () -> Unit,
    onDismissRequested: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequested,
        title = { Text(dialogHandle.templateName) },
        text = { Text(stringResource(R.string.quick_pick_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onLogAgainTapped) {
                Text(stringResource(R.string.quick_pick_confirm_log_again))
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenDetailsTapped) {
                Text(stringResource(R.string.quick_pick_confirm_open_details))
            }
        },
    )
}
