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
import dev.gaborbiro.dailymacros.features.modal.R
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
            .padding(horizontal = PaddingDefault)
            .padding(top = 12.dp, bottom = PaddingDefault),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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

@Composable
internal fun RecordDetailsViewBrowseButtons(
    onLogMealAgain: () -> Unit,
    onDismissRequested: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaddingDefault)
            .padding(top = 12.dp, bottom = PaddingDefault),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onLogMealAgain,
        ) {
            Text(
                text = stringResource(R.string.meal_details_action_add_new),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onDismissRequested,
        ) {
            Text(
                text = stringResource(R.string.meal_details_action_close),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
internal fun RecordDetailsViewEditButtons(
    primaryEnabled: Boolean,
    primaryLabel: String,
    onUpdate: () -> Unit,
    onSaveAndAdd: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaddingDefault)
            .padding(top = 12.dp, bottom = PaddingDefault),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onUpdate,
            enabled = primaryEnabled,
        ) {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSaveAndAdd,
            enabled = primaryEnabled,
        ) {
            Text(
                text = stringResource(R.string.meal_details_action_add_new_template),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onCancel,
        ) {
            Text(
                text = stringResource(R.string.meal_details_action_cancel),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
