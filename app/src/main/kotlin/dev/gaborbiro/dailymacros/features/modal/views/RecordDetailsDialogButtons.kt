package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.gaborbiro.dailymacros.design.PaddingDefault

@Composable
internal fun RecordDetailsDialogButtons(
    allowEdit: Boolean,
    saveButtonText: String,
    onDismissRequested: () -> Unit,
    onSubmitButtonTapped: () -> Unit,
) {
    Row(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(PaddingDefault),
        horizontalArrangement = Arrangement.End,
    ) {
        if (allowEdit) {
            TextButton(onDismissRequested) {
                Text(
                    modifier = Modifier.Companion
                        .padding(horizontal = PaddingDefault),
                    color = MaterialTheme.colorScheme.primary,
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            TextButton(onSubmitButtonTapped) {
                Text(
                    modifier = Modifier.Companion
                        .padding(horizontal = PaddingDefault),
                    color = MaterialTheme.colorScheme.primary,
                    text = saveButtonText,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            TextButton(onDismissRequested) {
                Text(
                    modifier = Modifier.Companion
                        .padding(horizontal = PaddingDefault),
                    color = MaterialTheme.colorScheme.primary,
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}