package dev.gaborbiro.dailymacros.features.common.views

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.gaborbiro.dailymacros.features.common.ErrorViewModel
import dev.gaborbiro.dailymacros.features.common.model.ErrorViewState
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingDouble

@Composable
fun ErrorDialog(viewModel: ErrorViewModel, error: ErrorViewState) {
    Dialog(onDismissRequest = { viewModel.onErrorDialogDismissRequested() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(PaddingDefault)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = error.message,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(PaddingDouble))

                Button(
                    onClick = { viewModel.onErrorDialogDismissRequested() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 40.dp)
                ) {
                    Text(text = "Ok")
                }
            }
        }
    }
}
