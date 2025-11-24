package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.model.DailyMacroProgressItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DailyMacroTitleView(
    modifier: Modifier,
    model: DailyMacroProgressItem,
) {
    Text(
        modifier = modifier
            .padding(start = PaddingHalf)
            .wrapContentHeight(),
        text = model.title,
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
}