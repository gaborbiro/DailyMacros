package dev.gaborbiro.dailymacros.features.widget.views

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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelBase
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelRecord
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelTemplate
import dev.gaborbiro.dailymacros.features.common.model.MacrosUIModel
import dev.gaborbiro.dailymacros.features.widget.WidgetActionProvider
import dev.gaborbiro.dailymacros.features.widget.WidgetActionProviderImpl
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetHalfVertical
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreview

@Composable
internal fun WidgetView(
    modifier: GlanceModifier,
    actionProvider: WidgetActionProvider,
    items: List<ListUIModelBase>,
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
                WidgetList(
                    items = items,
                    recordImageTapActionProvider = { recordId -> actionProvider.recordImageTapped(recordId) },
                    recordBodyTapActionProvider = { recordId -> actionProvider.recordBodyTapped(recordId) },
                    templateImageTapActionProvider = { templateId -> actionProvider.templateImageTapped(templateId) },
                    templateBodyTapActionProvider = { templateId -> actionProvider.templateBodyTapped(templateId) },
                    dismissQuickAddTooltipActionProvider = { actionProvider.dismissQuickAddTooltip() }
                )
            } else {
                EmptyView()
            }
            Box(
                modifier = GlanceModifier
                    .size(56.dp)
                    .padding(PaddingWidgetHalfVertical),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    modifier = GlanceModifier
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.tertiaryContainer)
                        .padding(12.dp)
                        .clickable(actionProvider.openApp())
                        .fillMaxSize(),
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
            launchNoteViaCameraAction = { actionProvider.createRecordWithCamera() },
            launchNewNoteViaImagePickerActionProvider = { actionProvider.createRecordWithImagePicker() },
            launchNewNoteViaTextOnlyActionProvider = { actionProvider.createRecordWithJustText() },
            reloadActionProvider = { actionProvider.reload() },
        )
    }
}

@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun WidgetViewPreview() {
    WidgetPreview {
        WidgetView(
            modifier = GlanceModifier
                .fillMaxSize(),
            actionProvider = WidgetActionProviderImpl(),
            items = listOf(
                ListUIModelRecord(
                    recordId = 1,
                    templateId = 1L,
                    title = "Breakfast",
                    timestamp = "Yesterday",
                    images = listOf("", ""),
                    macros = MacrosUIModel(
                        calories = "8cal",
                        protein = "prot 8",
                        fat = "fat 4(2)",
                        carbs = "carb 9(9)",
                        salt = "sal 2",
                        fibre = "fib 4",
                    ),
                ),
                ListUIModelTemplate(
                    templateId = 1,
                    title = "Breakfast",
                    description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                    images = listOf("", ""),
                ),
                ListUIModelTemplate(
                    templateId = 2,
                    title = "Lunch",
                    description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                    images = listOf("", ""),
                ),
                ListUIModelTemplate(
                    templateId = 3,
                    title = "Dinner",
                    description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                    images = listOf("", ""),
                ),
                ListUIModelRecord(
                    recordId = 2L,
                    templateId = 1L,
                    timestamp = "Yesterday",
                    title = "Lunch",
                    images = listOf("", ""),
                    macros = MacrosUIModel(
                        calories = "8cal",
                        protein = "prot 8",
                        fat = "fat 4(2)",
                        carbs = "carb 9(9)",
                        salt = "sal 2",
                        fibre = "fib 4",
                    ),
                ),
                ListUIModelRecord(
                    recordId = 3L,
                    templateId = 1L,
                    timestamp = "Yesterday",
                    title = "Dinner",
                    images = listOf("", ""),
                    macros = MacrosUIModel(
                        calories = "8cal",
                        protein = "prot 8",
                        fat = "fat 4(2)",
                        carbs = "carb 9(9)",
                        salt = "sal 2",
                        fibre = "fib 4",
                    ),
                ),
            ),
        )
    }
}

@Preview(widthDp = 156, heightDp = 180)
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun WidgetViewPreviewEmpty() {
    WidgetPreview {
        WidgetView(
            modifier = GlanceModifier
                .fillMaxSize(),
            actionProvider = WidgetActionProviderImpl(),
            items = emptyList(),
        )
    }
}
