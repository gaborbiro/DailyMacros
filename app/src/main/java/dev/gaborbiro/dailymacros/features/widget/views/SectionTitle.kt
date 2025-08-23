package dev.gaborbiro.dailymacros.features.widget.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
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
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetDefaultHorizontal
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetDefaultVertical
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreview

@Composable
fun SectionTitle(title: String) {
    Box(
        modifier = GlanceModifier
            .padding(horizontal = PaddingWidgetDefaultHorizontal),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            modifier = GlanceModifier
                .background(sectionTitleBackground)
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(horizontal = PaddingWidgetDefaultHorizontal, vertical = PaddingWidgetDefaultVertical)
                .cornerRadius(4.dp),
            text = title,
            style = sectionTitleTextStyle,
        )
    }
}


@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun SectionTitlePreview() {
    WidgetPreview {
        SectionTitle(
            title = "Favorites",
        )
    }
}
