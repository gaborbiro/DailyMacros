package dev.gaborbiro.dailymacros.features.widgets.quickpickwidget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import dev.gaborbiro.dailymacros.design.PaddingWidgetDefault
import dev.gaborbiro.dailymacros.features.widgets.model.ListUiModelQuickPick
import dev.gaborbiro.dailymacros.features.widgets.util.PreviewWidgetNavigator
import dev.gaborbiro.dailymacros.features.widgets.util.WidgetPreviewContext
import dev.gaborbiro.dailymacros.features.widgets.views.LocalImage
import dev.gaborbiro.dailymacros.features.widgets.views.OverlayTitleTextStyle

@Composable
internal fun QuickPickWidgetView(
    uiModel: ListUiModelQuickPick,
    onTapped: Action,
) {
    Box(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(6.dp)
            .fillMaxSize()
            .clickable(onTapped),
        contentAlignment = Alignment.BottomStart,
    ) {
        uiModel.imageFilename
            ?.let {
                LocalImage(
                    modifier = GlanceModifier.fillMaxSize(),
                    imageFilename = it,
                    contentScale = ContentScale.Crop,
                    thumbnail = false,
                )
            }
        Text(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Color(0x6F000000))
                .padding(PaddingWidgetDefault),
            text = uiModel.title,
            maxLines = 2,
            style = OverlayTitleTextStyle,
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 300, heightDp = 96)
@Composable
private fun QuickPickWidgetViewWithTitlePreview() {
    WidgetPreviewContext {
        QuickPickWidgetView(
            uiModel = ListUiModelQuickPick(
                title = "Breakfast",
                imageFilename = "",
                templateId = 0L,
                nutrients = null,
            ),
            onTapped = PreviewWidgetNavigator.quickPickBodyTapped(0L),
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 96, heightDp = 96)
@Composable
private fun QuickPickWidgetViewImageOnlyPreview() {
    WidgetPreviewContext {
        QuickPickWidgetView(
            uiModel = ListUiModelQuickPick(
                title = "Breakfast",
                imageFilename = "",
                templateId = 0L,
                nutrients = null,
            ),
            onTapped = PreviewWidgetNavigator.quickPickWidgetTapped(0L, "Breakfast"),
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 96, heightDp = 300)
@Composable
private fun QuickPickWidgetViewTallPreview() {
    WidgetPreviewContext {
        QuickPickWidgetView(
            uiModel = ListUiModelQuickPick(
                title = "Breakfast",
                imageFilename = "",
                templateId = 0L,
                nutrients = null,
            ),
            onTapped = PreviewWidgetNavigator.quickPickWidgetTapped(0L, "Breakfast"),
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 300, heightDp = 300)
@Composable
private fun QuickPickWidgetViewBigPreview() {
    WidgetPreviewContext {
        QuickPickWidgetView(
            uiModel = ListUiModelQuickPick(
                title = "Breakfast",
                imageFilename = "",
                templateId = 0L,
                nutrients = null,
            ),
            onTapped = PreviewWidgetNavigator.quickPickWidgetTapped(0L, "Breakfast"),
        )
    }
}
