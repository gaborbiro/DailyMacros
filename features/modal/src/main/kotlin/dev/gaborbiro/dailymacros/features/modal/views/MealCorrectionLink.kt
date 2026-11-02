package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.modal.R

/** Opens the existing meal editor at the description instead of introducing a second input. */
@Composable
internal fun MealCorrectionLink(onClick: () -> Unit) {
    Text(
        modifier = Modifier
            .padding(horizontal = PaddingHalf)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = PaddingHalf, vertical = 12.dp),
        text = stringResource(R.string.meal_details_correction_link),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}
