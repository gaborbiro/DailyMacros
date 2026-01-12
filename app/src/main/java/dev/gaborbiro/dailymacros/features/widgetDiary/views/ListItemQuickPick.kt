package dev.gaborbiro.dailymacros.features.widgetDiary.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import dev.gaborbiro.dailymacros.design.PaddingWidgetDefault
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelQuickPick
import dev.gaborbiro.dailymacros.features.common.model.MacrosAmountsUIModel
import dev.gaborbiro.dailymacros.features.widgetDiary.util.PreviewContext

@Composable
internal fun ListItemQuickPick(
    modifier: GlanceModifier = GlanceModifier,
    quickPickEntry: ListUIModelQuickPick,
    imageTapActionProvider: Action,
    bodyTapActionProvider: Action,
) {
    val extraPadding = remember { (WidgetImageSize - WidgetTemplateImageSize) / 2 }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(bodyTapActionProvider)
            .background(quickPickBackground)
            .then(
                GlanceModifier
                    .padding(start = extraPadding)
            ),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        quickPickEntry.images.firstOrNull()
            ?.let {
                LocalImage(
                    it,
                    modifier = GlanceModifier
                        .size(WidgetTemplateImageSize)
                        .clickable(imageTapActionProvider)
                        .cornerRadius(ListItemImageCornerRadius),
                    contentScale = ContentScale.Crop,
                )
            }
            ?: run {
                Spacer(modifier = GlanceModifier.size(WidgetTemplateImageSize))
            }
        Column {
            Text(
                modifier = GlanceModifier
                    .wrapContentHeight()
                    .padding(start = PaddingWidgetDefault + extraPadding, end = PaddingWidgetDefault),
                text = quickPickEntry.title,
                maxLines = 3,
                style = titleTextStyle,
            )
            quickPickEntry.macros?.calories
                ?.let {
                    Text(
                        modifier = GlanceModifier
                            .wrapContentHeight()
                            .padding(start = PaddingWidgetDefault + extraPadding, end = PaddingWidgetDefault),
                        text = it,
                        maxLines = 1,
                        style = descriptionTextStyle,
                    )
                }
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun ListItemQuickPickPreview() {
    PreviewContext {
        ListItemQuickPick(
            quickPickEntry = ListUIModelQuickPick(
                title = "Breakfast",
                images = listOf("", ""),
                templateId = 0L,
                macros = MacrosAmountsUIModel(
                    calories = "1008cal",
                    protein = "protein 8",
                    fat = "fat 4(2)",
                    carbs = "carbs 9(9)",
                    salt = "salt 2",
                    fibre = "fibre 4",
                ),
            ),
            imageTapActionProvider = action { },
            bodyTapActionProvider = action { },
        )
    }
}
