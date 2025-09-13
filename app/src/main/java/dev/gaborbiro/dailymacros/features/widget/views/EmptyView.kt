package dev.gaborbiro.dailymacros.features.widget.views

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
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreview

@Composable
internal fun EmptyView() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column {
            Text(
                modifier = GlanceModifier
                    .fillMaxWidth(),
                text = "Tap one of these buttons\nto record your first meal",
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
                    contentDescription = "Arrow to bottom-left",
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
    WidgetPreview {
        EmptyView()
    }
}
