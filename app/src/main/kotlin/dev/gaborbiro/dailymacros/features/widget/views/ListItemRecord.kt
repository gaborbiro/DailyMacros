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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import dev.gaborbiro.dailymacros.design.PaddingWidgetDefault
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelRecord
import dev.gaborbiro.dailymacros.features.common.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreviewContext

@Composable
fun ListItemRecord(
    modifier: GlanceModifier = GlanceModifier,
    record: ListUiModelRecord,
    imageTappedActionProvider: Action,
    bodyTappedActionProvider: Action,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(RecordBackground),
    ) {
        record.images.firstOrNull()
            ?.let {
                LocalImage(
                    it,
                    modifier = GlanceModifier
                        .size(WidgetImageSize)
                        .clickable(imageTappedActionProvider)
                        .cornerRadius(ListItemImageCornerRadius),
                    contentScale = ContentScale.Crop,
                )
            }
            ?: run {
                Spacer(modifier = GlanceModifier.size(WidgetImageSize))
            }
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .padding(horizontal = PaddingWidgetDefault)
                .clickable(bodyTappedActionProvider),
            verticalAlignment = Alignment.Vertical.Top,
        ) {
            Text(
                text = record.title,
                maxLines = if (record.showLoadingIndicator) 2 else 3,
                style = TitleTextStyle,
            )
            if (record.showLoadingIndicator) {
                Text(
                    text = "Analyzing…",
                    maxLines = 1,
                    style = LoadingTextStyle,
                )
            } else {
                val nutrient = record.nutrients?.calories?.let { " ($it)" }
                Text(
                    text = record.timestamp + nutrient,
                    maxLines = 1,
                    style = DateTextStyle,
                )
            }
        }
    }
}

@Preview(widthDp = 300)
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun RecordListItemPreview() {
    WidgetPreviewContext {
        ListItemRecord(
            record = ListUiModelRecord(
                recordId = 1,
                templateId = 1L,
                title = "Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast ",
                timestamp = "Yesterday",
                images = listOf("1", "2"),
                nutrients = NutrientsUiModel(
                    calories = "8cal",
                    protein = "prot 8",
                    fat = "fat 4(2)",
                    carbs = "carb 9(9)",
                    salt = "sal 2",
                    fibre = "fib 4",
                ),
                showLoadingIndicator = false,
                showAddToQuickPicksMenuItem = true,
            ),
            imageTappedActionProvider = action {},
            bodyTappedActionProvider = action {},
        )
    }
}

@Preview(widthDp = 300)
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun RecordListItemPreviewLoading() {
    WidgetPreviewContext {
        ListItemRecord(
            record = ListUiModelRecord(
                recordId = 1,
                templateId = 1L,
                title = "Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast ",
                timestamp = "Yesterday",
                images = listOf("1", "2"),
                nutrients = NutrientsUiModel(
                    calories = "8cal",
                    protein = "prot 8",
                    fat = "fat 4(2)",
                    carbs = "carb 9(9)",
                    salt = "sal 2",
                    fibre = "fib 4",
                ),
                showLoadingIndicator = true,
                showAddToQuickPicksMenuItem = false,
            ),
            imageTappedActionProvider = action {},
            bodyTappedActionProvider = action {},
        )
    }
}
