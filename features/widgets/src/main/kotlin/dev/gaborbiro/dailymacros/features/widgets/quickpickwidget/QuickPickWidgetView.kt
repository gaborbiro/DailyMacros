package dev.gaborbiro.dailymacros.features.widgets.quickpickwidget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.layout.wrapContentSize
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import dev.gaborbiro.dailymacros.design.PaddingWidgetDefault
import dev.gaborbiro.dailymacros.design.PaddingWidgetDouble
import dev.gaborbiro.dailymacros.design.PaddingWidgetHalf
import dev.gaborbiro.dailymacros.features.widgets.model.ListUiModelQuickPick
import dev.gaborbiro.dailymacros.features.widgets.util.PreviewWidgetNavigator
import dev.gaborbiro.dailymacros.features.widgets.util.WidgetPreviewContext
import dev.gaborbiro.dailymacros.features.widgets.views.LocalImage
import dev.gaborbiro.dailymacros.features.widgets.views.TitleTextStyle

@Composable
internal fun QuickPickWidgetView(
    uiModel: ListUiModelQuickPick,
    onTapped: Action,
) {
    Row(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(6.dp)
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(onTapped),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        uiModel.images.firstOrNull()
            ?.let {
                LocalImage(
                    it,
                    modifier = GlanceModifier
                        .wrapContentSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            ?: run {
                Spacer(modifier = GlanceModifier.size(PaddingWidgetDouble))
            }
        Text(
            modifier = GlanceModifier
                .wrapContentHeight()
                .padding(
                    horizontal = PaddingWidgetDefault,
                    vertical = PaddingWidgetDouble,
                ),
            text = uiModel.title,
            maxLines = 3,
            style = TitleTextStyle,
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 300)
@Composable
private fun QuickPickWidgetViewPreview() {
    WidgetPreviewContext {
        QuickPickWidgetView(
            uiModel = ListUiModelQuickPick(
                title = "Breakfast",
                images = listOf("", ""),
                templateId = 0L,
                nutrients = null,
            ),
            onTapped = PreviewWidgetNavigator.quickPickBodyTapped(0L),
        )
    }
}
