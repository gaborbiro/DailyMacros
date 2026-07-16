package dev.gaborbiro.dailymacros.features.widgets.diarywidget.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.LocalContext
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import dev.gaborbiro.dailymacros.features.widgets.R
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.widgets.util.WidgetPreviewContext

@Composable
internal fun EmptyView() {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column {
            Text(
                modifier = GlanceModifier
                    .fillMaxWidth(),
                text = context.getString(R.string.widgets_content_empty_state),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = GlanceTheme.colors.onBackground,
                ),
            )
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
            ) {
                Spacer(
                    modifier = GlanceModifier
                        .width(50.dp)
                )
                Image(
                    modifier = GlanceModifier
                        .height(70.dp)
                        .wrapContentWidth()
                        .padding(vertical = PaddingHalf),
                    provider = ImageProvider(R.drawable.ic_arrow),
                    contentDescription = context.getString(R.string.widgets_content_arrow_cd),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
                )
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 180, heightDp = 156)
@Composable
private fun EmptyViewPreview() {
    WidgetPreviewContext {
        EmptyView()
    }
}
