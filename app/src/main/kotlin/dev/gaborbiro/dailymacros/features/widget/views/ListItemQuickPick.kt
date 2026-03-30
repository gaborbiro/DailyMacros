package dev.gaborbiro.dailymacros.features.widget.views

import androidx.compose.runtime.Composable
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
import dev.gaborbiro.dailymacros.features.widget.model.ListUiModelQuickPick
import dev.gaborbiro.dailymacros.features.common.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreviewContext

@Composable
internal fun ListItemQuickPick(
    modifier: GlanceModifier = GlanceModifier,
    quickPickEntry: ListUiModelQuickPick,
    imageTapActionProvider: Action,
    bodyTapActionProvider: Action,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(bodyTapActionProvider)
            .background(QuickPickBackground),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        quickPickEntry.images.firstOrNull()
            ?.let {
                LocalImage(
                    it,
                    modifier = GlanceModifier
                        .size(WidgetImageSize)
                        .clickable(imageTapActionProvider)
                        .cornerRadius(ListItemImageCornerRadius),
                    contentScale = ContentScale.Crop,
                )
            }
            ?: run {
                Spacer(modifier = GlanceModifier.size(WidgetImageSize))
            }
        Column {
            Text(
                modifier = GlanceModifier
                    .wrapContentHeight()
                    .padding(start = PaddingWidgetDefault, end = PaddingWidgetDefault),
                text = quickPickEntry.title,
                maxLines = 3,
                style = TitleTextStyle,
            )
            quickPickEntry.nutrients?.calories
                ?.let {
                    Text(
                        modifier = GlanceModifier
                            .wrapContentHeight()
                            .padding(start = PaddingWidgetDefault, end = PaddingWidgetDefault),
                        text = it,
                        maxLines = 1,
                        style = DescriptionTextStyle,
                    )
                }
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun ListItemQuickPickPreview() {
    WidgetPreviewContext {
        ListItemQuickPick(
            quickPickEntry = ListUiModelQuickPick(
                title = "Breakfast",
                images = listOf("", ""),
                templateId = 0L,
                nutrients = NutrientsUiModel(
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
