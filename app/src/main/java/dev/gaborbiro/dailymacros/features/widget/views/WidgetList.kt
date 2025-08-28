package dev.gaborbiro.dailymacros.features.widget.views

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.action
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelBase
import dev.gaborbiro.dailymacros.features.common.model.MacrosUIModel
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelRecord
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetDefaultVertical
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetDoubleVertical
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelTemplate
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelTemplates
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelTemplatesStart
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreview

@Composable
internal fun WidgetList(
    items: List<ListUIModelBase>,
    recordImageTapActionProvider: @Composable (recordId: Long) -> Action,
    recordBodyTapActionProvider: @Composable (recordId: Long) -> Action,
    templateImageTapActionProvider: @Composable (templateId: Long) -> Action,
    templateBodyTapActionProvider: @Composable (templateId: Long) -> Action,
) {
    LazyColumn(
        modifier = GlanceModifier
            .fillMaxSize(),
    ) {
        itemsIndexed(
            items = items,
            itemId = { _, item -> item.listItemId },
        ) { index, item ->
            when (item) {
                is ListUIModelRecord -> {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                    ) {
                        Spacer(modifier = GlanceModifier.height(PaddingWidgetDefaultVertical))
                        ListItemRecord(
                            record = item,
                            imageTappedActionProvider = recordImageTapActionProvider(item.listItemId),
                            bodyTappedActionProvider = recordBodyTapActionProvider(item.listItemId),
                        )
                    }
                }

                is ListUIModelTemplate -> {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                    ) {
                        Spacer(modifier = GlanceModifier.height(PaddingWidgetDefaultVertical))
                        ListItemTemplate(
                            template = item,
                            imageTapActionProvider = templateImageTapActionProvider(item.templateId),
                            bodyTapActionProvider = templateBodyTapActionProvider(item.templateId),
                        )
                    }
                }

                is ListUIModelTemplatesStart -> {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                    ) {
                        Spacer(modifier = GlanceModifier.height(PaddingWidgetDoubleVertical))
                        SectionTitle(title = "Favorites")
                    }
                }

                is ListUIModelTemplates -> {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                    ) {
                        Spacer(modifier = GlanceModifier.height(PaddingWidgetDoubleVertical))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun RecordListPreviewExpanded() {
    WidgetPreview {
        WidgetList(
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
            recordImageTapActionProvider = { action {} },
            recordBodyTapActionProvider = { action {} },
            templateImageTapActionProvider = { action {} },
            templateBodyTapActionProvider = { action {} },
        )
    }
}
