package dev.gaborbiro.dailymacros.features.common.views

import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.gaborbiro.dailymacros.design.ViewPreviewContext

val dismissivePositiveButtons = arrayOf(
    "Fine",
    "Whatever",
    "Whatev",
    "Sure",
    "Yeah",
    "K",
    "OK then",
    "Alright",
    "If you say so",
    "Meh",
    "Cool, I guess",
    "Yup",
    "Right…",
    "Okie",
    "Okay fine",
    "So be it",
    "As you wish",
    "Ugh, fine"
)

@Composable
fun InfoDialog(message: String, onDismissRequested: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequested,
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismissRequested) { Text(dismissivePositiveButtons.random()) }
        },
    )
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun InfoDialogPreview() {
    ViewPreviewContext {
        InfoDialog("You can add as many images as you like. Nutritional labels are particularly useful. You can also add more pics later, don't worry about gathering all info right away.") { }
    }
}
