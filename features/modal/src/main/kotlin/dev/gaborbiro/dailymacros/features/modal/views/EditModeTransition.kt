package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How long the meal details dialog takes to turn into its edit form, and back. */
internal const val EditModeTransitionMillis = 300

internal fun <T> editModeSpec(): FiniteAnimationSpec<T> = tween(durationMillis = EditModeTransitionMillis)

/** For parts of the meal details dialog that exist in only one mode: they grow or shrink in place while fading. */
internal val editModeEnter: EnterTransition = fadeIn(editModeSpec()) + expandVertically(editModeSpec())
internal val editModeExit: ExitTransition = fadeOut(editModeSpec()) + shrinkVertically(editModeSpec())

/** Matches the content padding of a Material text field without a label. */
private val FieldContentPadding = 16.dp

/**
 * Text that turns into a text field in place. While browsing it reads as plain text; switching to [editing]
 * fades in the field's container, underline and [trailingIcon], and eases the text into the field's padding,
 * so the dialog changes mode without swapping one layout for another.
 */
@Composable
internal fun InPlaceTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    editing: Boolean,
    textStyle: TextStyle,
    browseTextColor: Color,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    trailingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    editingMinHeight: Dp = 0.dp,
) {
    val progress by animateFloatAsState(
        targetValue = if (editing) 1f else 0f,
        animationSpec = editModeSpec(),
        label = "inPlaceTextFieldEditing",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = TextFieldDefaults.colors()
    val containerColor = colors.unfocusedContainerColor
    val indicatorColor = when {
        isError -> colors.errorIndicatorColor
        focused -> colors.focusedIndicatorColor
        else -> colors.unfocusedIndicatorColor
    }
    val indicatorThickness = if (focused) 2.dp else 1.dp
    val focusManager = LocalFocusManager.current

    LaunchedEffect(editing) {
        if (!editing) focusManager.clearFocus()
    }

    Row(
        modifier = modifier
            .heightIn(min = editingMinHeight * progress)
            .background(containerColor.copy(alpha = containerColor.alpha * progress), TextFieldDefaults.shape)
            .drawBehind {
                val strokePx = indicatorThickness.toPx()
                val y = size.height - strokePx / 2
                drawLine(
                    color = indicatorColor.copy(alpha = indicatorColor.alpha * progress),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokePx,
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            modifier = Modifier
                .weight(1f)
                // A tall description field starts typing at the top, like a regular text field.
                .align(Alignment.Top)
                .padding(
                    horizontal = FieldContentPadding * progress,
                    vertical = FieldContentPadding * progress,
                )
                .let {
                    // A disabled field would be announced as one; while browsing this is just text.
                    if (editing) it else it.clearAndSetSemantics { contentDescription = value.text }
                },
            value = value,
            onValueChange = onValueChange,
            enabled = editing,
            textStyle = textStyle.copy(color = lerp(browseTextColor, colors.focusedTextColor, progress)),
            cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
            keyboardOptions = keyboardOptions,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box {
                    if (editing && value.text.isEmpty()) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = placeholder,
                            style = MaterialTheme.typography.labelLarge,
                            fontStyle = FontStyle.Italic,
                            color = Color.Gray,
                        )
                    }
                    innerTextField()
                }
            },
        )
        AnimatedVisibility(
            visible = editing,
            enter = fadeIn(editModeSpec()) + expandHorizontally(editModeSpec()),
            exit = fadeOut(editModeSpec()) + shrinkHorizontally(editModeSpec()),
        ) {
            trailingIcon()
        }
    }
}
