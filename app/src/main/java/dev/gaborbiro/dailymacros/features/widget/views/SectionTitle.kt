package dev.gaborbiro.dailymacros.features.widget.views

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentHeight
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetDefault
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetDouble
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetHalf
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreview

@Composable
fun SectionTitle(title: String, @DrawableRes trailingImage: Int? = null, onClick: () -> Unit) {
    Box(
        modifier = GlanceModifier
            .padding(top = PaddingWidgetDefault)
            .padding(horizontal = PaddingWidgetDefault)
            .clickable(onClick),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = title,
            modifier = GlanceModifier
                .background(sectionTitleBackground)
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(horizontal = PaddingWidgetDouble, vertical = PaddingWidgetHalf)
                .cornerRadius(4.dp),
            style = sectionTitleTextStyle,
        )
        trailingImage?.let {
            Image(
                modifier = GlanceModifier
                    .padding(end = PaddingWidgetDefault),
                provider = ImageProvider(trailingImage),
                contentDescription = "",
            )
        }
    }
}


@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun SectionTitlePreview() {
    WidgetPreview {
        SectionTitle(
            title = "Top Meals",
            trailingImage = R.drawable.keyboard_arrow_down,
            onClick = { },
        )
    }
}
