package dev.gaborbiro.dailymacros.features.widgets.quickpickwidget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.layout.wrapContentSize
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import dev.gaborbiro.dailymacros.design.PaddingWidgetDefault
import dev.gaborbiro.dailymacros.design.PaddingWidgetDouble
import dev.gaborbiro.dailymacros.features.widgets.model.ListUiModelQuickPick
import dev.gaborbiro.dailymacros.features.widgets.util.PreviewWidgetNavigator
import dev.gaborbiro.dailymacros.features.widgets.util.WidgetPreviewContext
import dev.gaborbiro.dailymacros.features.widgets.views.LocalImage
import dev.gaborbiro.dailymacros.features.widgets.views.TitleTextStyle

private val MinimumUsefulImageSize = 56.dp
private val MinimumReadableTitleWidth = 64.dp
private val MinimumReadableTitleHeight = 24.dp

private enum class WidgetLayout { IMAGE_ONLY, LANDSCAPE, PORTRAIT }

@Composable
internal fun QuickPickWidgetView(
    uiModel: ListUiModelQuickPick,
    onTapped: Action,
) {
    val widgetSize = LocalSize.current
    when (determineLayout(widgetSize.width, widgetSize.height)) {
        WidgetLayout.LANDSCAPE -> QuickPickWidgetWithTitle(
            uiModel = uiModel,
            onTapped = onTapped,
            imageSize = widgetSize.height,
        )
        WidgetLayout.PORTRAIT -> QuickPickWidgetWithTitlePortrait(
            uiModel = uiModel,
            onTapped = onTapped,
            imageSize = widgetSize.width,
        )
        WidgetLayout.IMAGE_ONLY -> QuickPickWidgetImageOnly(
            uiModel = uiModel,
            onTapped = onTapped,
        )
    }
}

private fun determineLayout(widgetWidth: Dp, widgetHeight: Dp): WidgetLayout {
    return if (widgetHeight > widgetWidth) {
        // Portrait: image fills width (square crop), text below
        val titleHeight = widgetHeight - widgetWidth - PaddingWidgetDouble * 2
        if (widgetWidth >= MinimumUsefulImageSize && titleHeight >= MinimumReadableTitleHeight) {
            WidgetLayout.PORTRAIT
        } else {
            WidgetLayout.IMAGE_ONLY
        }
    } else {
        // Landscape: image fills height (square crop), text to the right
        val titleWidth = widgetWidth - widgetHeight - PaddingWidgetDefault * 2
        val titleHeight = widgetHeight - PaddingWidgetDouble * 2
        if (widgetHeight >= MinimumUsefulImageSize &&
            titleWidth >= MinimumReadableTitleWidth &&
            titleHeight >= MinimumReadableTitleHeight
        ) {
            WidgetLayout.LANDSCAPE
        } else {
            WidgetLayout.IMAGE_ONLY
        }
    }
}

@Composable
private fun QuickPickWidgetWithTitle(
    uiModel: ListUiModelQuickPick,
    onTapped: Action,
    imageSize: Dp,
) {
    Row(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(6.dp)
            .fillMaxSize()
            .clickable(onTapped),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        uiModel.imageFilenames.firstOrNull()
            ?.let {
                LocalImage(
                    it,
                    modifier = GlanceModifier.size(imageSize),
                    contentScale = ContentScale.Crop,
                    thumbnail = false,
                )
            }
            ?: run {
                Spacer(modifier = GlanceModifier.size(imageSize))
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

@Composable
private fun QuickPickWidgetWithTitlePortrait(
    uiModel: ListUiModelQuickPick,
    onTapped: Action,
    imageSize: Dp,
) {
    Column(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(6.dp)
            .fillMaxSize()
            .clickable(onTapped),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        uiModel.imageFilenames.firstOrNull()
            ?.let {
                LocalImage(
                    it,
                    modifier = GlanceModifier.size(imageSize),
                    contentScale = ContentScale.Crop,
                    thumbnail = false,
                )
            }
            ?: run {
                Spacer(modifier = GlanceModifier.size(imageSize))
            }
        Text(
            modifier = GlanceModifier
                .wrapContentSize()
                .padding(
                    horizontal = PaddingWidgetDefault,
                    vertical = PaddingWidgetDefault,
                ),
            text = uiModel.title,
            maxLines = 2,
            style = TitleTextStyle,
        )
    }
}

@Composable
private fun QuickPickWidgetImageOnly(
    uiModel: ListUiModelQuickPick,
    onTapped: Action,
) {
    Box(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(6.dp)
            .fillMaxSize()
            .clickable(onTapped),
    ) {
        uiModel.imageFilenames.firstOrNull()
            ?.let {
                LocalImage(
                    modifier = GlanceModifier.fillMaxSize(),
                    imageFilename = it,
                    contentScale = ContentScale.Crop,
                    thumbnail = false,
                )
            }
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
                imageFilenames = listOf("", ""),
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
                imageFilenames = listOf("", ""),
                templateId = 0L,
                nutrients = null,
            ),
            onTapped = PreviewWidgetNavigator.quickPickWidgetTapped(0L),
        )
    }
}
