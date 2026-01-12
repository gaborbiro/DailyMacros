package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.ViewPreviewContext

@Composable
internal fun PillLabel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    text: String,
    onClick: (() -> Unit)? = null, // if null, it's non-clickable/label-style
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    contentColor: Color = MaterialTheme.colorScheme.primary,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    elevation: Dp = 0.dp,
    enabled: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
    iconOrientation: Orientation = Orientation.Horizontal
) {
    val shape = RoundedCornerShape(16.dp) // pill

    val clickableModifier = if (onClick != null && enabled) {
        Modifier.clickable(
            onClick = onClick,
            indication = LocalIndication.current,
            interactionSource = remember { MutableInteractionSource() }
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .then(clickableModifier),
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        tonalElevation = elevation,
        border = border,
        shadowElevation = elevation
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 24.dp) // make it thinner than default buttons
                .padding(contentPadding),
            verticalAlignment = Alignment.Top,
        ) {
            AxisLayout(orientation = iconOrientation) {
                Icon(
                    modifier = Modifier
                        .size(16.dp),
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Insert to input field",
                )
                Icon(
                    modifier = Modifier
                        .size(16.dp),
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = "Insert to input field",
                )
            }
            Text(
                modifier = Modifier
                    .padding(start = PaddingHalf),
                text = text,
                style = textStyle,
            )
        }
    }
}

enum class Orientation {
    Horizontal, Vertical
}

@Composable
private fun AxisLayout(
    orientation: Orientation,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    when (orientation) {
        Orientation.Horizontal -> Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }

        Orientation.Vertical -> Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PillLabelPreview() {
    ViewPreviewContext {
        PillLabel(
            text = "This is a label",
            onClick = {},
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PillLabelPreviewLong() {
    ViewPreviewContext {
        PillLabel(
            text = "A packaged meal of chicken teriyaki with jasmine rice and oriental mix, high in carbs and protein,  with 484 kcal.",
            onClick = {},
            iconOrientation = Orientation.Vertical,
        )
    }
}