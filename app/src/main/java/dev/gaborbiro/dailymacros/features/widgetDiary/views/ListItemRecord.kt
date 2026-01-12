package dev.gaborbiro.dailymacros.features.widgetDiary.views

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
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
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelRecord
import dev.gaborbiro.dailymacros.features.common.model.MacrosAmountsUIModel
import dev.gaborbiro.dailymacros.features.widgetDiary.util.PreviewContext

@Composable
fun ListItemRecord(
    record: ListUIModelRecord,
    imageTappedActionProvider: Action,
    bodyTappedActionProvider: Action,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(start = PaddingWidgetDefault),
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
                style = titleTextStyle,
            )
            if (record.showLoadingIndicator) {
                Text(
                    text = "Analyzing…",
                    maxLines = 1,
                    style = loadingTextStyle,
                )
            } else {
                val nutrient = record.macrosAmounts?.calories?.let { " ($it)" }
                Text(
                    text = record.timestamp + nutrient,
                    maxLines = 1,
                    style = dateTextStyle,
                )
            }
        }
    }
}

@Preview(widthDp = 300)
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun RecordListItemPreview() {
    PreviewContext {
        ListItemRecord(
            record = ListUIModelRecord(
                recordId = 1,
                templateId = 1L,
                title = "Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast ",
                timestamp = "Yesterday",
                images = listOf("1", "2"),
                macrosAmounts = MacrosAmountsUIModel(
                    calories = "8cal",
                    protein = "prot 8",
                    fat = "fat 4(2)",
                    carbs = "carb 9(9)",
                    salt = "sal 2",
                    fibre = "fib 4",
                ),
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
    PreviewContext {
        ListItemRecord(
            record = ListUIModelRecord(
                recordId = 1,
                templateId = 1L,
                title = "Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast Breakfast ",
                timestamp = "Yesterday",
                images = listOf("1", "2"),
                macrosAmounts = MacrosAmountsUIModel(
                    calories = "8cal",
                    protein = "prot 8",
                    fat = "fat 4(2)",
                    carbs = "carb 9(9)",
                    salt = "sal 2",
                    fibre = "fib 4",
                ),
                showLoadingIndicator = true,
            ),
            imageTappedActionProvider = action {},
            bodyTappedActionProvider = action {},
        )
    }
}
