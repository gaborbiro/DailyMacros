package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.PaddingDefault

@Composable
internal fun RecordDetailsDialogButtons(
    showCloseOnly: Boolean,
    showSaveAndAdd: Boolean,
    primaryEnabled: Boolean = true,
    primaryLabel: String?,
    saveAndAddLabel: String?,
    onDismissRequested: () -> Unit,
    onSaveTapped: () -> Unit,
    onSaveAndAddTapped: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingDefault),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showCloseOnly) {
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismissRequested,
            ) {
                Text(
                    text = stringResource(R.string.meal_details_action_close),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSaveTapped,
                enabled = primaryEnabled,
            ) {
                Text(
                    text = primaryLabel.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (showSaveAndAdd && saveAndAddLabel != null) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSaveAndAddTapped,
                ) {
                    Text(
                        text = saveAndAddLabel,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismissRequested,
            ) {
                Text(
                    text = stringResource(R.string.meal_details_action_cancel),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
