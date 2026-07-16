package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onOpenDetailsTapped,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.quick_pick_confirm_open_details))
                }
                Button(
                    onClick = onLogAgainTapped,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.quick_pick_confirm_log_again))
                }
            }
        },
    )
}
