package dev.gaborbiro.dailymacros.features.common.view

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingDouble

@Composable
fun ConfirmDeleteNutrientDataDialog(
    onConfirm: () -> Unit,
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
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "This will delete macronutrient information. You can re-query it from the app, with the \"\uD83C\uDF4ENutrients\" menu item.",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(PaddingDouble))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismissRequested) {
                        Text("Cancel")
                    }
                    TextButton(onClick = onConfirm) {
                        Text("OK")
                    }
                }
            }
        }
    }
}


@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ConfirmDestructiveChangeDialogPreview() {
    DailyMacrosTheme {
        ConfirmDeleteNutrientDataDialog(
            onConfirm = {},
            onDismissRequested = {},
        )
    }
}
