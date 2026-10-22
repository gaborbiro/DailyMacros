package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
 *
 * At rest it is a single link, so it doesn't compete with the results. Tapping it opens the field; the
 * Re-analyze button only appears once there is something to send.
 */
@Composable
internal fun MealCorrectionInput(
    correction: TextFieldValue,
    canReanalyze: Boolean,
    onCorrectionChanged: (TextFieldValue) -> Unit,
    onReanalyzeTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(correction.text.isNotBlank()) }
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var fieldFocused by remember { mutableStateOf(false) }
    var openHeightPx by remember { mutableIntStateOf(0) }
    val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
    // The field scrolls itself into view when it gains focus, but that's before the keyboard is up. The dialog's
    // scroll area then shrinks frame by frame as the keyboard rises (and grows again when Re-analyze slides in),
    // so keep asking for the field and its button to be in view while it's focused and either of those changes.
    LaunchedEffect(fieldFocused, imeBottomPx, openHeightPx) {
        if (fieldFocused) bringIntoViewRequester.bringIntoView()
    }
    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = !open) {
            // A bare link rather than a TextButton: it lines up with the text above (TextButton's own inset
            // would push it off the 16dp edge) and doesn't pad the bottom of the dialog with its 48dp height.
            Text(
                modifier = Modifier
                    .padding(horizontal = PaddingHalf)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { open = true }
                    .padding(horizontal = PaddingHalf, vertical = 12.dp),
                text = stringResource(R.string.meal_details_correction_link),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        AnimatedVisibility(visible = open) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onSizeChanged { openHeightPx = it.height }
                    .padding(top = PaddingDefault),
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaddingDefault)
                        .focusRequester(focusRequester)
                        .onFocusChanged { fieldFocused = it.isFocused },
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
                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.End),
                    visible = canReanalyze,
                ) {
                    TextButton(
                        modifier = Modifier.padding(horizontal = PaddingDefault),
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
            // Opened by tapping the link (rather than restored with text in it): put the cursor in the field.
            if (correction.text.isEmpty()) {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MealCorrectionInputClosedPreview() {
    ViewPreviewContext {
        MealCorrectionInput(
            correction = TextFieldValue(),
            canReanalyze = false,
            onCorrectionChanged = {},
            onReanalyzeTapped = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MealCorrectionInputPreview() {
    ViewPreviewContext {
        MealCorrectionInput(
            correction = TextFieldValue("It was brown rice, about 200 g"),
            canReanalyze = true,
            onCorrectionChanged = {},
            onReanalyzeTapped = {},
        )
    }
}
