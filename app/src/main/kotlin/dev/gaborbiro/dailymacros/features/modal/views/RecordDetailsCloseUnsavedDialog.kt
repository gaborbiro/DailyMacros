package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.gaborbiro.dailymacros.R

@Composable
internal fun RecordDetailsCloseUnsavedDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(R.string.meal_details_close_unsaved_title)) },
        text = { Text(stringResource(R.string.meal_details_close_unsaved_message)) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.meal_details_close_unsaved_discard))
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) {
                Text(stringResource(R.string.meal_details_close_unsaved_keep_editing))
            }
        },
    )
}
