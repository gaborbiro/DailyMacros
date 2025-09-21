package dev.gaborbiro.dailymacros.features.common.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingDouble

@Composable
fun ErrorDialog(errorMessage: String, onDismissRequested: () -> Unit) {
    Dialog(onDismissRequested) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(PaddingDefault)
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = errorMessage,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                )

                Spacer(modifier = Modifier.height(PaddingDouble))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 40.dp),
                    onClick = onDismissRequested,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    shape = RoundedCornerShape(50.dp),
                ) {
                    Text(text = "Ok")
                }
            }
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun InfoDialogPreview() {
    AppTheme {
        ErrorDialog("You can add as many images as you like. Nutritional labels are particularly useful. You can also add more pics later, don't worry about gathering all info right away.") { }
    }
}
