package dev.gaborbiro.dailymacros.features.widgets.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import dev.gaborbiro.dailymacros.features.widgets.R
import dev.gaborbiro.dailymacros.design.PaddingWidgetDouble
import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelBase
import dev.gaborbiro.dailymacros.features.widgets.model.ListUiModelQuickPick
import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelRecord
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.features.widgets.WidgetNavigator
import dev.gaborbiro.dailymacros.features.widgets.util.PreviewWidgetNavigator
import dev.gaborbiro.dailymacros.features.widgets.util.WidgetPreviewContext

@Composable
internal fun DiaryWidgetView(
    modifier: GlanceModifier,
    navigator: WidgetNavigator,
    items: List<ListUiModelBase>,
) {
    Column(
        modifier = modifier
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight(),
            contentAlignment = Alignment.BottomEnd,
        ) {
            if (items.isNotEmpty()) {
                DiaryWidgetList(
                    items = items,
                    recordImageTapActionProvider = { recordId -> navigator.recordImageTapped(recordId) },
                    recordBodyTapActionProvider = { recordId -> navigator.recordBodyTapped(recordId) },
                    quickPickImageTapActionProvider = { templateId -> navigator.quickPickImageTapped(templateId) },
                    quickPickBodyTapActionProvider = { templateId -> navigator.quickPickBodyTapped(templateId) },
                )
            } else {
                EmptyView()
            }
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        ImageProvider(R.drawable.widget_shadow)
                    )
            ) {}
            Box(
                modifier = GlanceModifier
                    .padding(PaddingWidgetDouble),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    modifier = GlanceModifier
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.tertiaryContainer)
                        .padding(12.dp)
                        .clickable(navigator.openApp())
                        .size(48.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                    provider = ImageProvider(R.drawable.ic_open_in_new),
                    contentDescription = "open app",
                )
            }
        }
        ButtonLayout(
            modifier = GlanceModifier
                .fillMaxWidth()
                .wrapContentHeight(),
            launchNoteViaCameraAction = { navigator.createRecordWithCamera() },
            launchNewNoteViaImagePickerActionProvider = { navigator.createRecordWithImagePicker() },
            launchNewNoteViaTextOnlyActionProvider = { navigator.createRecord() },
            reloadActionProvider = { navigator.reload() },
        )
    }
}

@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun WidgetViewPreview() {
    WidgetPreviewContext {
        DiaryWidgetView(
            modifier = GlanceModifier
                .fillMaxSize(),
            navigator = PreviewWidgetNavigator,
            items = listOf(
                ListUiModelRecord(
                    recordId = 1,
                    templateId = 1L,
                    title = "Breakfast",
                    timestamp = "Yesterday",
                    imageFilename = "",
                    nutrients = NutrientsUiModel(
                        calories = "8cal",
                        protein = "prot 8",
                        fat = "fat 4(2)",
                        carbs = "carb 9(9)",
                        salt = "sal 2",
                        fibre = "fib 4",
                    ),
                    showLoadingIndicator = false,
                ),
                ListUiModelQuickPick(
                    templateId = 1,
                    title = "Breakfast",
                    imageFilename = "",
                    nutrients = NutrientsUiModel(
                        calories = "1008cal",
                        protein = "protein 8",
                        fat = "fat 4(2)",
                        carbs = "carbs 9(9)",
                        salt = "salt 2",
                        fibre = "fibre 4",
                    ),
                ),
                ListUiModelQuickPick(
                    templateId = 2,
                    title = "Lunch",
                    imageFilename = "",
                    nutrients = NutrientsUiModel(
                        calories = "1008cal",
                        protein = "protein 8",
                        fat = "fat 4(2)",
                        carbs = "carbs 9(9)",
                        salt = "salt 2",
                        fibre = "fibre 4",
                    ),
                ),
                ListUiModelQuickPick(
                    templateId = 3,
                    title = "Dinner",
                    imageFilename = "",
                    nutrients = NutrientsUiModel(
                        calories = "1008cal",
                        protein = "protein 8",
                        fat = "fat 4(2)",
                        carbs = "carbs 9(9)",
                        salt = "salt 2",
                        fibre = "fibre 4",
                    ),
                ),
                ListUiModelRecord(
                    recordId = 2L,
                    templateId = 1L,
                    timestamp = "Yesterday",
                    title = "Lunch",
                    imageFilename = "",
                    nutrients = NutrientsUiModel(
                        calories = "8cal",
                        protein = "prot 8",
                        fat = "fat 4(2)",
                        carbs = "carb 9(9)",
                        salt = "sal 2",
                        fibre = "fib 4",
                    ),
                    showLoadingIndicator = false,
                ),
                ListUiModelRecord(
                    recordId = 3L,
                    templateId = 1L,
                    timestamp = "Yesterday",
                    title = "Dinner",
                    imageFilename = "",
                    nutrients = NutrientsUiModel(
                        calories = "8cal",
                        protein = "prot 8",
                        fat = "fat 4(2)",
                        carbs = "carb 9(9)",
                        salt = "sal 2",
                        fibre = "fib 4",
                    ),
                    showLoadingIndicator = false,
                ),
            ),
        )
    }
}

@Preview(widthDp = 156, heightDp = 180)
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun WidgetViewPreviewEmpty() {
    WidgetPreviewContext {
        DiaryWidgetView(
            modifier = GlanceModifier
                .fillMaxSize(),
            navigator = PreviewWidgetNavigator,
            items = emptyList(),
        )
    }
}
