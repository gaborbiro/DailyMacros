package dev.gaborbiro.dailymacros.features.widgets.diarywidget.views

import androidx.compose.runtime.Composable
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.LocalContext
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentWidth
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import dev.gaborbiro.dailymacros.features.widgets.R
import dev.gaborbiro.dailymacros.design.PaddingWidgetDefault
import dev.gaborbiro.dailymacros.features.widgets.util.WidgetPreviewContext
import dev.gaborbiro.dailymacros.features.widgets.diarywidget.QuickPickBackground
import dev.gaborbiro.dailymacros.features.widgets.diarywidget.SectionTitleTextStyle

@Composable
fun ListItemQuickPickHeader(
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
    ) {
        Row(
            modifier = GlanceModifier
                .background(QuickPickBackground)
                .padding(top = PaddingWidgetDefault)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_star),
                contentDescription = context.getString(R.string.widgets_content_favorite_cd),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            )
            Text(
                modifier = GlanceModifier
                    .padding(PaddingWidgetDefault)
                    .wrapContentWidth(),
                text = context.getString(R.string.widgets_content_quick_picks_header),
                style = SectionTitleTextStyle,
            )
            Image(
                provider = ImageProvider(R.drawable.ic_star),
                contentDescription = context.getString(R.string.widgets_content_favorite_cd),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            )
        }
    }
}


@Preview(widthDp = 256)
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun ListItemQuickPickPreview() {
    WidgetPreviewContext {
        ListItemQuickPickHeader()
    }
}
