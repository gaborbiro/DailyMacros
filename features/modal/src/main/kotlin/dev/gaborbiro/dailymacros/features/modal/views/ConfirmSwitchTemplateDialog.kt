package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.gaborbiro.dailymacros.features.modal.R

@Composable
internal fun ConfirmSwitchTemplateDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.meal_details_switch_variant_title)) },
        text = { Text(stringResource(R.string.meal_details_switch_variant_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.meal_details_switch_variant_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.meal_details_switch_variant_cancel))
            }
        },
    )
}
