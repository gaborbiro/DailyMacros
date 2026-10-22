package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.modal.R

/**
 * "Something off?" under the AI's findings: what the user types is added to the meal's description and the
 * meal is analyzed again, without leaving the dialog.
 */
@Composable
internal fun MealCorrectionInput(
    correction: TextFieldValue,
    canReanalyze: Boolean,
    onCorrectionChanged: (TextFieldValue) -> Unit,
    onReanalyzeTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = PaddingHalf),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            modifier = Modifier.padding(horizontal = PaddingHalf, vertical = PaddingHalf),
            text = stringResource(R.string.meal_details_correction_prompt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingHalf),
            value = correction,
            onValueChange = onCorrectionChanged,
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = {
                Text(
                    text = stringResource(R.string.meal_details_correction_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray,
                )
            },
            trailingIcon = {
                VoiceInputButton(
                    prompt = stringResource(R.string.meal_details_correction_voice_prompt),
                    contentDescription = stringResource(R.string.meal_details_correction_voice_cd),
                    onTextRecognised = { spoken ->
                        onCorrectionChanged(appendSpokenText(correction, spoken))
                    },
                )
            },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
            ),
        )
        OutlinedButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = PaddingHalf, vertical = PaddingHalf),
            enabled = canReanalyze,
            onClick = onReanalyzeTapped,
        ) {
            AiActionIcon()
            Text(
                text = stringResource(R.string.meal_details_correction_reanalyze),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MealCorrectionInputPreview() {
    ViewPreviewContext {
        MealCorrectionInput(
            modifier = Modifier.padding(vertical = PaddingDefault),
            correction = TextFieldValue("It was brown rice, about 200 g"),
            canReanalyze = true,
            onCorrectionChanged = {},
            onReanalyzeTapped = {},
        )
    }
}
