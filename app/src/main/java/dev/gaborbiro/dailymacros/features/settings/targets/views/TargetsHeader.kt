package dev.gaborbiro.dailymacros.features.settings.targets.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.ViewPreviewContext


@Composable
internal fun TargetsHeader(
    modifier: Modifier = Modifier,
    saveButtonEnabled: Boolean,
    resetButtonVisible: Boolean,
    onSaveTapped: () -> Unit,
    onResetTapped: () -> Unit,
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                text = "Daily Targets",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Start
            )
            if (resetButtonVisible) {
                TextButton(
                    modifier = Modifier
                        .padding(end = 8.dp),
                    onClick = onResetTapped,
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold ),
                    )
                }
            }
            TextButton(
                onClick = onSaveTapped,
                enabled = saveButtonEnabled,
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold ),
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(top = 8.dp)
        ) {
            Text(
                text = "Feel free to disable the ones you don't care about",
            )
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TargetsHeaderPreview() {
    ViewPreviewContext {
        TargetsHeader(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            saveButtonEnabled = false,
            resetButtonVisible = false,
            onSaveTapped = {},
            onResetTapped = {},
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TargetsHeaderPreviewStrict() {
    ViewPreviewContext {
        TargetsHeader(
            modifier = Modifier
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp),
            saveButtonEnabled = true,
            resetButtonVisible = true,
            onSaveTapped = {},
            onResetTapped = {},
        )
    }
}
