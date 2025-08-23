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
import dev.gaborbiro.dailymacros.features.common.model.BaseListItemUIModel
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetDefaultVertical
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetDoubleVertical
import dev.gaborbiro.dailymacros.features.widget.model.TemplateUIModel
import dev.gaborbiro.dailymacros.features.widget.model.TemplatesEndUIModel
import dev.gaborbiro.dailymacros.features.widget.model.TemplatesStartUIModel
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreview

@Composable
internal fun WidgetList(
    items: List<BaseListItemUIModel>,
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
            itemId = { _, item -> item.id },
        ) { index, item ->
            when (item) {
                is RecordUIModel -> {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                    ) {
                        Spacer(modifier = GlanceModifier.height(PaddingWidgetDefaultVertical))
                        ListItemRecord(
                            record = item,
                            imageTappedActionProvider = recordImageTapActionProvider(item.id),
                            bodyTappedActionProvider = recordBodyTapActionProvider(item.id),
                        )
                    }
                }

                is TemplateUIModel -> {
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

                is TemplatesStartUIModel -> {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                    ) {
                        Spacer(modifier = GlanceModifier.height(PaddingWidgetDoubleVertical))
                        SectionTitle(title = "Favorites")
                    }
                }

                is TemplatesEndUIModel -> {
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
                RecordUIModel(
                    recordId = 1,
                    templateId = 1L,
                    title = "Breakfast",
                    description = "8cal, Prot 8, Carb 9, Sug 9, Fat 4, Sat 2, Sal: 0",
                    timestamp = "Yesterday",
                    images = listOf("", ""),
                    hasMacros = true,
                ),
                TemplateUIModel(
                    templateId = 1,
                    title = "Breakfast",
                    description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                    images = listOf("", ""),
                ),
                TemplateUIModel(
                    templateId = 2,
                    title = "Lunch",
                    description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                    images = listOf("", ""),
                ),
                TemplateUIModel(
                    templateId = 3,
                    title = "Dinner",
                    description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                    images = listOf("", ""),
                ),
                RecordUIModel(
                    recordId = 2L,
                    templateId = 1L,
                    timestamp = "Yesterday",
                    title = "Lunch",
                    description = "8cal, Prot 8, Carb 9, Sug 9, Fat 4, Sat 2, Sal: 0",
                    images = listOf("", ""),
                    hasMacros = true,
                ),
                RecordUIModel(
                    recordId = 3L,
                    templateId = 1L,
                    timestamp = "Yesterday",
                    title = "Dinner",
                    description = "8cal, Prot 8, Carb 9, Sug 9, Fat 4, Sat 2, Sal: 0",
                    images = listOf("", ""),
                    hasMacros = true,
                ),
            ),
            recordImageTapActionProvider = { action {} },
            recordBodyTapActionProvider = { action {} },
            templateImageTapActionProvider = { action {} },
            templateBodyTapActionProvider = { action {} },
        )
    }
}
