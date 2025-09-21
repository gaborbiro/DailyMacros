package dev.gaborbiro.dailymacros.features.widget.views

import androidx.compose.runtime.Composable
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentWidth
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetDefaultVertical
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreview

@Composable
fun ListItemQuickPickHeader(
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_star),
            contentDescription = "Favorite",
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
        )
        Text(
            modifier = GlanceModifier
                .padding(PaddingWidgetDefaultVertical)
                .wrapContentWidth(),
            text = "Tap to Quick Pick",
            style = sectionTitleTextStyle,
        )
        Image(
            provider = ImageProvider(R.drawable.ic_star),
            contentDescription = "Favorite",
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
        )
    }
}


@Preview(widthDp = 256)
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun ListItemQuickPickPreview() {
    WidgetPreview {
        ListItemQuickPickHeader()
    }
}
