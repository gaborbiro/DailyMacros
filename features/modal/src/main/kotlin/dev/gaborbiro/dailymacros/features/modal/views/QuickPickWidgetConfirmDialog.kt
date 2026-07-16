package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    onDontShowAgainChanged: (Boolean) -> Unit,
    onDismissRequested: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequested,
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = dialogHandle.templateName,
                    modifier = Modifier.padding(end = 36.dp),
                )
                IconButton(
                    onClick = onDismissRequested,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 12.dp, y = (-12).dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.quick_pick_confirm_close_cd),
                    )
                }
            }
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDontShowAgainChanged(!dialogHandle.dontShowAgain) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = dialogHandle.dontShowAgain,
                    onCheckedChange = onDontShowAgainChanged,
                )
                Text(
                    text = stringResource(R.string.quick_pick_confirm_dont_show_again),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
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
