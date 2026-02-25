package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext

@Composable
internal fun OutlinedText(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    text: String,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    contentColor: Color = MaterialTheme.colorScheme.primary,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    elevation: Dp = 0.dp,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
) {
    val shape = RoundedCornerShape(16.dp) // pill

    Surface(
        modifier = modifier,
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
            Text(
                modifier = Modifier
                    .padding(horizontal = PaddingHalf),
                text = text,
                style = textStyle,
            )
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OutlinedTextPreview() {
    ViewPreviewContext {
        OutlinedText(
            text = "This is a label",
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OutlinedTextPreviewLong() {
    ViewPreviewContext {
        OutlinedText(
            text = "A packaged meal of chicken teriyaki with jasmine rice and oriental mix, high in carbs and protein,  with 484 kcal.",
        )
    }
}