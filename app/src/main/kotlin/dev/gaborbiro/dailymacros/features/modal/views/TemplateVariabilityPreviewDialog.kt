package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext

@Composable
fun TemplateVariabilityPreviewDialog(
    message: String,
    onAddConfirmed: () -> Unit,
    onDismissRequested: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequested) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(PaddingDefault)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = "Variability for this template",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(PaddingDefault))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAddConfirmed,
                    colors = normalButtonColors,
                    shape = RoundedCornerShape(50.dp),
                ) {
                    Text(text = "Add")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismissRequested,
                ) {
                    Text(text = "Cancel")
                }
            }
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TemplateVariabilityPreviewDialogPreview() {
    ViewPreviewContext {
        TemplateVariabilityPreviewDialog(
            message = "Archetype: Toast (toast)\n  Slot: Spread (spread)\n    • Butter (butter)",
            onAddConfirmed = {},
            onDismissRequested = {},
        )
    }
}
