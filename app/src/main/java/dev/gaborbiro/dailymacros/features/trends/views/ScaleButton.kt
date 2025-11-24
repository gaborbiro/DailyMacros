package dev.gaborbiro.dailymacros.features.trends.views

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun ScaleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        modifier = modifier
            .height(36.dp)
            .fillMaxWidth(),
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = label,
                textAlign = TextAlign.Center
            )
        },
    )
}
